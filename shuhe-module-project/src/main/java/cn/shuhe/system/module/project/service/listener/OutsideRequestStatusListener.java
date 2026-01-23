package cn.shuhe.system.module.project.service.listener;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.OutsideRequestMapper;
import cn.shuhe.system.module.project.service.OutsideRequestService;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.project.service.ServiceItemService;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.service.dingtalk.OutsideConfirmService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 外出请求发起流程审批结果的监听器
 * 
 * 当审批通过后，更新外出请求状态，并设置外出人员
 */
@Component
@Slf4j
public class OutsideRequestStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 外出请求发起的流程定义 Key
     */
    public static final String PROCESS_DEFINITION_KEY = "outside_request_start";

    @Resource
    private OutsideRequestService outsideRequestService;

    @Resource
    private OutsideRequestMapper outsideRequestMapper;

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private ProjectService projectService;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private OutsideConfirmService outsideConfirmService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("【外出请求监听】收到流程状态变更事件，processInstanceId={}, status={}",
                event.getId(), event.getStatus());

        String processInstanceId = event.getId();

        // 从流程变量中获取外出请求ID
        Map<String, Object> processVariables = event.getProcessVariables();
        Long outsideRequestId = null;
        if (processVariables != null && processVariables.get("outsideRequestId") != null) {
            Object idObj = processVariables.get("outsideRequestId");
            if (idObj instanceof Number) {
                outsideRequestId = ((Number) idObj).longValue();
            } else {
                try {
                    outsideRequestId = Long.parseLong(idObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("【外出请求监听】解析 outsideRequestId 失败: {}", idObj);
                }
            }
        }

        // 查找对应的外出请求
        OutsideRequestDO request = null;
        if (outsideRequestId != null) {
            request = outsideRequestMapper.selectById(outsideRequestId);
        }
        if (request == null) {
            // 尝试通过流程实例ID查找
            request = outsideRequestMapper.selectByProcessInstanceId(processInstanceId);
        }
        if (request == null) {
            log.warn("【外出请求监听】未找到对应的外出请求，processInstanceId={}, outsideRequestId={}",
                    processInstanceId, outsideRequestId);
            return;
        }

        // 更新流程实例ID（如果还没有设置）
        if (request.getProcessInstanceId() == null || request.getProcessInstanceId().isEmpty()) {
            OutsideRequestDO updateObj = new OutsideRequestDO();
            updateObj.setId(request.getId());
            updateObj.setProcessInstanceId(processInstanceId);
            outsideRequestMapper.updateById(updateObj);
            request.setProcessInstanceId(processInstanceId);
        }

        try {
            // 根据流程状态更新外出请求状态
            if (BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
                // 审批通过
                handleApproved(request, event);
            } else if (BpmTaskStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
                // 审批拒绝
                handleRejected(request);
            } else if (BpmTaskStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
                // 流程取消
                handleCancelled(request);
            } else {
                log.info("【外出请求监听】流程状态变更，暂不处理。status={}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("【外出请求监听】处理流程状态变更失败。processInstanceId={}, error={}",
                    processInstanceId, e.getMessage(), e);
        }
    }

    /**
     * 处理审批通过
     */
    private void handleApproved(OutsideRequestDO request, BpmProcessInstanceStatusEvent event) {
        log.info("【外出请求监听】审批通过，更新状态。requestId={}", request.getId());

        // 更新状态为已通过
        outsideRequestService.updateOutsideRequestStatus(request.getId(), 1);

        // 将关联的服务项设置为可见，并更新项目状态为进行中
        if (request.getServiceItemId() != null) {
            try {
                // 1. 更新服务项可见性
                serviceItemService.updateServiceItemVisible(request.getServiceItemId(), 1);
                log.info("【外出请求监听】已将服务项 {} 设置为可见", request.getServiceItemId());
                
                // 2. 获取服务项关联的项目，更新项目状态为进行中
                ServiceItemDO serviceItem = serviceItemService.getServiceItem(request.getServiceItemId());
                if (serviceItem != null && serviceItem.getProjectId() != null) {
                    projectService.updateProjectStatus(serviceItem.getProjectId(), 1); // 1=进行中
                    log.info("【外出请求监听】已将项目 {} 状态更新为进行中", serviceItem.getProjectId());
                }
            } catch (Exception e) {
                log.error("【外出请求监听】更新服务项/项目状态失败: {}", e.getMessage(), e);
            }
        }

        // 从流程变量中获取外出人员（如果审批时选择了）
        Map<String, Object> processVariables = event.getProcessVariables();
        List<Long> memberUserIds = parseMemberUserIds(processVariables);
        
        if (CollUtil.isNotEmpty(memberUserIds)) {
            // 设置外出人员
            outsideRequestService.setOutsideMembers(request.getId(), memberUserIds);
            log.info("【外出请求监听】设置外出人员成功。requestId={}, memberCount={}",
                    request.getId(), memberUserIds.size());

            // 发送钉钉通知给被选中的外出人员
            sendDingtalkNotifyToMembers(request, memberUserIds, processVariables);
        }
    }

    /**
     * 从流程变量中解析外出人员ID列表
     */
    private List<Long> parseMemberUserIds(Map<String, Object> processVariables) {
        if (processVariables == null) {
            return new ArrayList<>();
        }

        Object memberUserIdsObj = processVariables.get("memberUserIds");
        if (memberUserIdsObj == null) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();
        
        if (memberUserIdsObj instanceof List) {
            // 处理 List 类型
            for (Object item : (List<?>) memberUserIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (memberUserIdsObj instanceof Collection) {
            // 处理其他 Collection 类型
            for (Object item : (Collection<?>) memberUserIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (memberUserIdsObj.getClass().isArray()) {
            // 处理数组类型
            Object[] array = (Object[]) memberUserIdsObj;
            for (Object item : array) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        }

        return result;
    }

    /**
     * 转换为 Long
     */
    private Long convertToLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("【外出请求监听】转换用户ID失败: {}", obj);
            return null;
        }
    }

    /**
     * 发送钉钉通知给外出人员（使用互动卡片，带确认按钮）
     */
    private void sendDingtalkNotifyToMembers(OutsideRequestDO request, List<Long> memberUserIds, 
                                              Map<String, Object> processVariables) {
        try {
            // 获取项目名称和其他信息
            String projectName = getStringFromVariables(processVariables, "projectName", "未知项目");
            String destination = request.getDestination() != null ? request.getDestination() : "未指定";
            String planStartTime = formatDateTime(request.getPlanStartTime());
            String planEndTime = formatDateTime(request.getPlanEndTime());
            String reason = request.getReason() != null ? request.getReason() : "无";

            String title = "外出任务通知";
            String content = String.format(
                    "**%s**\n\n" +
                    "您已被安排参加外出任务，请点击下方按钮确认。\n\n" +
                    "- 项目名称：%s\n" +
                    "- 外出地点：%s\n" +
                    "- 计划时间：%s ~ %s\n" +
                    "- 外出事由：%s\n\n" +
                    "**确认后将自动为您提交钉钉外出申请。**",
                    title,
                    projectName,
                    destination,
                    planStartTime,
                    planEndTime,
                    reason
            );

            // 获取刚刚保存的外出人员记录，为每个人生成确认链接
            List<cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO> members = 
                    outsideRequestService.getOutsideMembers(request.getId());
            
            if (CollUtil.isEmpty(members)) {
                log.warn("【外出请求监听】未找到外出人员记录，无法发送带确认按钮的通知。requestId={}", request.getId());
                // 回退到普通通知
                sendPlainNotify(memberUserIds, title, content);
                return;
            }

            // 为每个外出人员单独发送带确认按钮的消息
            for (cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO member : members) {
                try {
                    // 生成确认链接
                    String confirmUrl = generateConfirmUrl(member.getId(), member.getUserId());
                    
                    // 发送 ActionCard 消息
                    boolean success = dingtalkNotifyApi.sendActionCardMessage(
                            java.util.Collections.singletonList(member.getUserId()),
                            title,
                            content,
                            "确认外出",
                            confirmUrl
                    );
                    
                    if (success) {
                        log.info("【外出请求监听】发送钉钉ActionCard通知成功。requestId={}, userId={}", 
                                request.getId(), member.getUserId());
                    } else {
                        log.warn("【外出请求监听】发送钉钉ActionCard通知失败。requestId={}, userId={}", 
                                request.getId(), member.getUserId());
                    }
                } catch (Exception e) {
                    log.error("【外出请求监听】发送钉钉ActionCard通知异常。requestId={}, userId={}, error={}",
                            request.getId(), member.getUserId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("【外出请求监听】发送钉钉通知异常。requestId={}, error={}",
                    request.getId(), e.getMessage(), e);
        }
    }

    /**
     * 生成确认链接URL
     * 使用 OutsideConfirmService 的方法，确保URL格式正确（包含 /admin-api 前缀和从数据库读取配置）
     */
    private String generateConfirmUrl(Long memberId, Long userId) {
        return outsideConfirmService.generateConfirmUrl(memberId, userId);
    }

    /**
     * 发送普通通知（降级方案）
     */
    private void sendPlainNotify(List<Long> userIds, String title, String content) {
        DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                .setUserIds(userIds)
                .setTitle(title)
                .setContent(content);
        dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);
    }

    /**
     * 从流程变量中获取字符串
     */
    private String getStringFromVariables(Map<String, Object> variables, String key, String defaultValue) {
        if (variables == null || !variables.containsKey(key)) {
            return defaultValue;
        }
        Object value = variables.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未指定";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 处理审批拒绝
     */
    private void handleRejected(OutsideRequestDO request) {
        log.info("【外出请求监听】审批拒绝，更新状态。requestId={}", request.getId());
        outsideRequestService.updateOutsideRequestStatus(request.getId(), 2);
    }

    /**
     * 处理流程取消
     */
    private void handleCancelled(OutsideRequestDO request) {
        log.info("【外出请求监听】流程取消，更新状态。requestId={}", request.getId());
        outsideRequestService.updateOutsideRequestStatus(request.getId(), 4);
    }

}
