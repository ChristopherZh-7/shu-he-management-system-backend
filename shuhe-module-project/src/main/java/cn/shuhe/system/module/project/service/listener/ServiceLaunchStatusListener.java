package cn.shuhe.system.module.project.service.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMemberMapper;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.project.service.ServiceItemService;
import cn.shuhe.system.module.project.service.ServiceLaunchService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.dict.DictDataApi;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.service.dingtalk.ServiceLaunchConfirmService;
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
 * 统一服务发起流程审批结果的监听器
 * 
 * 当审批通过后，更新服务发起状态，创建执行轮次，并发送钉钉通知
 */
@Component
@Slf4j
public class ServiceLaunchStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 统一服务发起的流程定义 Key
     */
    public static final String PROCESS_DEFINITION_KEY = "unified_service_launch";

    @Resource
    private ServiceLaunchService serviceLaunchService;

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private ProjectService projectService;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private ServiceLaunchMemberMapper serviceLaunchMemberMapper;

    @Resource
    private ServiceLaunchConfirmService serviceLaunchConfirmService;

    @Resource
    private DictDataApi dictDataApi;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("【服务发起监听】收到流程状态变更事件，processInstanceId={}, status={}",
                event.getId(), event.getStatus());

        String processInstanceId = event.getId();

        // 从流程变量中获取服务发起ID
        Map<String, Object> processVariables = event.getProcessVariables();
        Long serviceLaunchId = null;
        if (processVariables != null && processVariables.get("serviceLaunchId") != null) {
            Object idObj = processVariables.get("serviceLaunchId");
            if (idObj instanceof Number) {
                serviceLaunchId = ((Number) idObj).longValue();
            } else {
                try {
                    serviceLaunchId = Long.parseLong(idObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("【服务发起监听】解析 serviceLaunchId 失败: {}", idObj);
                }
            }
        }

        // 查找对应的服务发起记录
        ServiceLaunchDO launch = null;
        if (serviceLaunchId != null) {
            launch = serviceLaunchMapper.selectById(serviceLaunchId);
        }
        if (launch == null) {
            // 尝试通过流程实例ID查找
            launch = serviceLaunchMapper.selectOne(
                    ServiceLaunchDO::getProcessInstanceId, processInstanceId);
        }
        if (launch == null) {
            log.warn("【服务发起监听】未找到对应的服务发起记录，processInstanceId={}, serviceLaunchId={}",
                    processInstanceId, serviceLaunchId);
            return;
        }

        // 更新流程实例ID（如果还没有设置）
        if (launch.getProcessInstanceId() == null || launch.getProcessInstanceId().isEmpty()) {
            serviceLaunchService.updateProcessInstanceId(launch.getId(), processInstanceId);
            launch.setProcessInstanceId(processInstanceId);
        }

        try {
            // 根据流程状态更新服务发起状态
            if (BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
                // 审批通过
                handleApproved(launch, event);
            } else if (BpmTaskStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
                // 审批拒绝
                handleRejected(launch);
            } else if (BpmTaskStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
                // 流程取消
                handleCancelled(launch);
            } else {
                log.info("【服务发起监听】流程状态变更，暂不处理。status={}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("【服务发起监听】处理流程状态变更失败。processInstanceId={}, error={}",
                    processInstanceId, e.getMessage(), e);
        }
    }

    /**
     * 处理审批通过
     */
    private void handleApproved(ServiceLaunchDO launch, BpmProcessInstanceStatusEvent event) {
        log.info("【服务发起监听】审批通过，更新状态。launchId={}", launch.getId());

        // 从流程变量中获取执行人员（如果审批时选择了）
        Map<String, Object> processVariables = event.getProcessVariables();
        List<Long> executorUserIds = parseExecutorUserIds(processVariables);

        // 调用 Service 处理审批通过逻辑（更新状态、创建轮次等）
        serviceLaunchService.handleApproved(launch.getId(), executorUserIds);

        // 更新服务项可见性和项目状态
        if (launch.getServiceItemId() != null) {
            try {
                // 更新服务项可见性
                serviceItemService.updateServiceItemVisible(launch.getServiceItemId(), 1);
                log.info("【服务发起监听】已将服务项 {} 设置为可见", launch.getServiceItemId());

                // 获取服务项关联的项目，更新项目状态为进行中
                ServiceItemDO serviceItem = serviceItemService.getServiceItem(launch.getServiceItemId());
                if (serviceItem != null && serviceItem.getProjectId() != null) {
                    projectService.updateProjectStatus(serviceItem.getProjectId(), 1); // 1=进行中
                    log.info("【服务发起监听】已将项目 {} 状态更新为进行中", serviceItem.getProjectId());
                }
            } catch (Exception e) {
                log.error("【服务发起监听】更新服务项/项目状态失败: {}", e.getMessage(), e);
            }
        }

        // 如果是外出服务，创建执行人记录并发送带确认按钮的钉钉通知
        if (Boolean.TRUE.equals(launch.getIsOutside()) && CollUtil.isNotEmpty(executorUserIds)) {
            createLaunchMembersAndNotify(launch, executorUserIds, processVariables);
        } else if (CollUtil.isNotEmpty(executorUserIds)) {
            // 非外出服务，发送普通钉钉通知
            sendDingtalkNotifyToExecutors(launch, executorUserIds, processVariables);
        }
    }

    /**
     * 创建服务发起执行人记录并发送带确认按钮的钉钉通知（外出服务专用）
     */
    private void createLaunchMembersAndNotify(ServiceLaunchDO launch, List<Long> executorUserIds,
                                               Map<String, Object> processVariables) {
        log.info("【服务发起监听】外出服务，创建执行人记录。launchId={}, executorCount={}",
                launch.getId(), executorUserIds.size());

        // 获取用户信息
        List<AdminUserRespDTO> users = adminUserApi.getUserList(executorUserIds);
        if (CollUtil.isEmpty(users)) {
            log.warn("【服务发起监听】未找到执行人用户信息。executorUserIds={}", executorUserIds);
            return;
        }

        // 创建执行人记录
        for (AdminUserRespDTO user : users) {
            try {
                // 获取部门信息
                String deptName = null;
                if (user.getDeptId() != null) {
                    DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                    if (dept != null) {
                        deptName = dept.getName();
                    }
                }

                ServiceLaunchMemberDO member = ServiceLaunchMemberDO.builder()
                        .launchId(launch.getId())
                        .userId(user.getId())
                        .userName(user.getNickname())
                        .userDeptId(user.getDeptId())
                        .userDeptName(deptName)
                        .confirmStatus(0) // 未确认
                        .finishStatus(0)  // 未完成
                        .build();
                serviceLaunchMemberMapper.insert(member);

                // 发送带确认按钮的钉钉通知
                sendActionCardNotifyToMember(launch, member, processVariables);

                log.info("【服务发起监听】创建执行人记录成功。launchId={}, userId={}, memberId={}",
                        launch.getId(), user.getId(), member.getId());
            } catch (Exception e) {
                log.error("【服务发起监听】创建执行人记录失败。launchId={}, userId={}, error={}",
                        launch.getId(), user.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 发送带确认按钮的钉钉ActionCard通知
     */
    private void sendActionCardNotifyToMember(ServiceLaunchDO launch, ServiceLaunchMemberDO member,
                                               Map<String, Object> processVariables) {
        try {
            String customerName = getStringFromVariables(processVariables, "customerName", "未知客户");
            String serviceType = getStringFromVariables(processVariables, "serviceType", null);
            
            // 获取服务项的部门类型（用于选择正确的字典）
            Integer deptType = null;
            if (launch.getServiceItemId() != null) {
                ServiceItemDO serviceItem = serviceItemService.getServiceItem(launch.getServiceItemId());
                if (serviceItem != null) {
                    deptType = serviceItem.getDeptType();
                }
            }
            
            // 获取服务类型的中文名称（需要根据部门类型选择正确的字典）
            String serviceTypeName = getServiceTypeLabel(serviceType, deptType);
            if (StrUtil.isEmpty(serviceTypeName)) {
                serviceTypeName = getStringFromVariables(processVariables, "serviceItemName", "未知服务项");
            }
            
            String planStartTime = formatDateTime(launch.getPlanStartTime());
            String planEndTime = formatDateTime(launch.getPlanEndTime());
            String destination = launch.getDestination() != null ? launch.getDestination() : "未指定";
            String reason = launch.getReason() != null ? launch.getReason() : "无";

            String title = "外出任务通知";
            String content = String.format(
                    "**%s**\n\n" +
                    "您已被安排参加外出任务，请点击下方按钮确认。\n\n" +
                    "- 服务项：%s\n" +
                    "- 客户：%s\n" +
                    "- 外出地点：%s\n" +
                    "- 计划时间：%s ~ %s\n" +
                    "- 外出事由：%s\n\n" +
                    "**确认后将自动为您提交钉钉外出申请。**",
                    title,
                    serviceTypeName,
                    customerName,
                    destination,
                    planStartTime,
                    planEndTime,
                    reason
            );

            // 生成确认链接
            String confirmUrl = serviceLaunchConfirmService.generateConfirmUrl(member.getId(), member.getUserId());

            // 发送 ActionCard 消息
            boolean success = dingtalkNotifyApi.sendActionCardMessage(
                    java.util.Collections.singletonList(member.getUserId()),
                    title,
                    content,
                    "确认外出",
                    confirmUrl
            );

            if (success) {
                log.info("【服务发起监听】发送钉钉ActionCard通知成功。launchId={}, userId={}",
                        launch.getId(), member.getUserId());
            } else {
                log.warn("【服务发起监听】发送钉钉ActionCard通知失败。launchId={}, userId={}",
                        launch.getId(), member.getUserId());
            }
        } catch (Exception e) {
            log.error("【服务发起监听】发送钉钉ActionCard通知异常。launchId={}, userId={}, error={}",
                    launch.getId(), member.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * 获取服务类型的中文名称（从字典）
     */
    /**
     * 部门类型对应的服务类型字典映射
     * 1 - 安全服务
     * 2 - 安全运营
     * 3 - 数据安全
     */
    private static final java.util.Map<Integer, String> DEPT_TYPE_SERVICE_DICT_MAP = java.util.Map.of(
            1, "project_service_type_security",
            2, "project_service_type_operation",
            3, "project_service_type_data"
    );

    /**
     * 获取服务类型的中文名称（从字典）
     * 
     * @param serviceType 服务类型代码（如 penetration_test）
     * @param deptType 部门类型（1-安全服务, 2-安全运营, 3-数据安全）
     * @return 服务类型中文名称（如 渗透测试）
     */
    private String getServiceTypeLabel(String serviceType, Integer deptType) {
        if (StrUtil.isEmpty(serviceType)) {
            return null;
        }
        
        // 根据部门类型获取对应的字典类型
        String dictType = deptType != null ? DEPT_TYPE_SERVICE_DICT_MAP.get(deptType) : null;
        if (StrUtil.isEmpty(dictType)) {
            log.warn("【服务发起监听】未知的部门类型，无法获取服务类型字典。deptType={}", deptType);
            return serviceType;
        }
        
        try {
            List<DictDataRespDTO> dictDataList = dictDataApi.getDictDataList(dictType);
            if (dictDataList != null) {
                for (DictDataRespDTO dictData : dictDataList) {
                    if (serviceType.equals(dictData.getValue())) {
                        return dictData.getLabel();
                    }
                }
            }
            log.warn("【服务发起监听】在字典 {} 中未找到服务类型 {}", dictType, serviceType);
        } catch (Exception e) {
            log.warn("【服务发起监听】获取服务类型中文名称失败，serviceType={}, dictType={}, error={}", 
                    serviceType, dictType, e.getMessage());
        }
        return serviceType; // 找不到则返回原值
    }

    /**
     * 从流程变量中解析执行人员ID列表
     */
    private List<Long> parseExecutorUserIds(Map<String, Object> processVariables) {
        if (processVariables == null) {
            return new ArrayList<>();
        }

        // 尝试多个可能的变量名
        Object executorIdsObj = processVariables.get("executorUserIds");
        if (executorIdsObj == null) {
            executorIdsObj = processVariables.get("executorIds");
        }
        if (executorIdsObj == null) {
            executorIdsObj = processVariables.get("memberUserIds");
        }
        if (executorIdsObj == null) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();

        if (executorIdsObj instanceof List) {
            for (Object item : (List<?>) executorIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (executorIdsObj instanceof Collection) {
            for (Object item : (Collection<?>) executorIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (executorIdsObj.getClass().isArray()) {
            Object[] array = (Object[]) executorIdsObj;
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
            log.warn("【服务发起监听】转换用户ID失败: {}", obj);
            return null;
        }
    }

    /**
     * 发送钉钉通知给执行人员
     */
    private void sendDingtalkNotifyToExecutors(ServiceLaunchDO launch, List<Long> executorUserIds,
                                                Map<String, Object> processVariables) {
        try {
            // 获取相关信息
            String serviceItemName = getStringFromVariables(processVariables, "serviceItemName", "未知服务项");
            String customerName = getStringFromVariables(processVariables, "customerName", "未知客户");
            String executeDeptName = getStringFromVariables(processVariables, "executeDeptName", "未知部门");
            String planStartTime = formatDateTime(launch.getPlanStartTime());
            String planEndTime = formatDateTime(launch.getPlanEndTime());
            
            Boolean isOutside = launch.getIsOutside();
            Boolean isCrossDept = launch.getIsCrossDept();

            // 构建通知标题
            String title;
            if (Boolean.TRUE.equals(isOutside)) {
                title = "外出服务任务通知";
            } else if (Boolean.TRUE.equals(isCrossDept)) {
                title = "跨部门服务任务通知";
            } else {
                title = "服务任务通知";
            }

            // 构建通知内容
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("**").append(title).append("**\n\n");
            contentBuilder.append("您已被安排执行以下服务任务：\n\n");
            contentBuilder.append("- 服务项：").append(serviceItemName).append("\n");
            contentBuilder.append("- 客户：").append(customerName).append("\n");
            contentBuilder.append("- 执行部门：").append(executeDeptName).append("\n");
            contentBuilder.append("- 计划时间：").append(planStartTime).append(" ~ ").append(planEndTime).append("\n");

            if (Boolean.TRUE.equals(isOutside)) {
                String destination = launch.getDestination() != null ? launch.getDestination() : "未指定";
                String reason = launch.getReason() != null ? launch.getReason() : "无";
                contentBuilder.append("- 外出地点：").append(destination).append("\n");
                contentBuilder.append("- 外出事由：").append(reason).append("\n");
            }

            if (Boolean.TRUE.equals(isCrossDept)) {
                contentBuilder.append("\n**注意：这是一个跨部门服务任务。**\n");
            }

            if (launch.getRemark() != null && !launch.getRemark().isEmpty()) {
                contentBuilder.append("\n备注：").append(launch.getRemark()).append("\n");
            }

            String content = contentBuilder.toString();

            // 发送钉钉通知
            DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                    .setUserIds(executorUserIds)
                    .setTitle(title)
                    .setContent(content);
            dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);

            log.info("【服务发起监听】发送钉钉通知成功。launchId={}, executorCount={}",
                    launch.getId(), executorUserIds.size());

        } catch (Exception e) {
            log.error("【服务发起监听】发送钉钉通知异常。launchId={}, error={}",
                    launch.getId(), e.getMessage(), e);
        }
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
    private void handleRejected(ServiceLaunchDO launch) {
        log.info("【服务发起监听】审批拒绝，更新状态。launchId={}", launch.getId());
        serviceLaunchService.handleRejected(launch.getId());
    }

    /**
     * 处理流程取消
     */
    private void handleCancelled(ServiceLaunchDO launch) {
        log.info("【服务发起监听】流程取消，更新状态。launchId={}", launch.getId());
        serviceLaunchService.handleCancelled(launch.getId());
    }

}
