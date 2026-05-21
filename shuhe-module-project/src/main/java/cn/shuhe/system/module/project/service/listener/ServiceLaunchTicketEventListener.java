package cn.shuhe.system.module.project.service.listener;

import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.service.ServiceLaunchService;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 监听工单接单事件，针对 {@code business_type=service_launch} 的工单自动落地：
 *
 * <ol>
 *   <li>从 {@code ticket.ext_json} 提取业务参数 contractId / serviceItemId / executeDeptId 等</li>
 *   <li>跳过 BPM 直接创建 {@link ServiceLaunchDO}（status=0）</li>
 *   <li>调 {@link ServiceLaunchService#handleApproved} 完成「设置执行人 + 创建轮次 + 回写 round_id」</li>
 *   <li>把生成的 {@code service_launch.id} 回写到 {@code ticket.business_id}</li>
 * </ol>
 *
 * <p>同步执行；任意失败直接冒泡，导致 {@code acceptTicket} 事务整体回滚（工单不进入处理中状态）。
 *
 * <p>仅处理 {@link TicketBusinessTypeEnum#SERVICE_LAUNCH}；其它业务类型在本 listener 中静默跳过。
 */
@Component
@Slf4j
public class ServiceLaunchTicketEventListener {

    // ext_json 字段名约定（与前端 assembleSaveReq 保持一致）
    private static final String EXT_PROJECT_ID = "projectId";
    private static final String EXT_DEPT_TYPE = "deptType";
    private static final String EXT_SERVICE_TYPE = "serviceType";
    private static final String EXT_EXECUTE_DEPT_ID = "executeDeptId";
    private static final String EXT_IS_OUTSIDE = "isOutside";
    private static final String EXT_DESTINATION = "destination";
    private static final String EXT_REASON = "reason";
    private static final String EXT_PLAN_START_TIME = "planStartTime";
    private static final String EXT_PLAN_END_TIME = "planEndTime";

    @Resource
    private ServiceLaunchService serviceLaunchService;

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private TicketMapper ticketMapper;

    @EventListener
    public void onTicketAccepted(TicketAcceptedEvent event) {
        if (!TicketBusinessTypeEnum.SERVICE_LAUNCH.getType().equals(event.getBusinessType())) {
            return;
        }
        Map<String, Object> ext = event.getExtJson();
        if (ext == null || ext.isEmpty()) {
            throw new IllegalArgumentException(
                    "service_launch 类型工单 ext_json 为空，无法落 service_launch（ticketId=" + event.getTicketId() + ")");
        }

        ServiceLaunchSaveReqVO req = buildServiceLaunchReq(event, ext);

        // 1. 创建 service_launch（业务侧自带 isBpmServiceLaunchEnabled 开关；走该开关在 createServiceLaunch
        //    内部决定要不要起 BPM。Listener 不强制改写该开关 —— 由部署方在 application.yml 控制）
        Long launchId = serviceLaunchService.createServiceLaunch(req);
        log.info("【工单接单·服务派遣】已创建 service_launch，launchId={} ticketId={}",
                launchId, event.getTicketId());

        // 2. 走「审批通过」路径：写执行人 + 创建 round + 回写 round_id（+ 跨部门成本 / 外出记录）
        Long roundId = serviceLaunchService.handleApproved(launchId, event.getExecutorIds());
        log.info("【工单接单·服务派遣】已 handleApproved，launchId={} roundId={} executors={}",
                launchId, roundId, event.getExecutorIds());

        // 3. 回写 ticket.business_id 让前端能从工单跳转到 service_launch 详情
        TicketDO ticketUpdate = new TicketDO();
        ticketUpdate.setId(event.getTicketId());
        ticketUpdate.setBusinessId(launchId);
        ticketMapper.updateById(ticketUpdate);
    }

    /**
     * 根据 ext_json 反查服务项并装配 ServiceLaunchSaveReqVO。
     *
     * <p>用户在工单表单只填项目 + 部门类型 + 服务类型 + 执行部门；
     * listener 自己根据 projectId + deptType + serviceType 在 project_info 中找匹配的服务项 id。
     * 找不到立即抛 {@link IllegalArgumentException}，由 {@code TicketServiceImpl.acceptTicket} 包成
     * {@code TICKET_DRIVER_FAILED} 错误码回滚事务。
     */
    private ServiceLaunchSaveReqVO buildServiceLaunchReq(TicketAcceptedEvent event,
                                                          Map<String, Object> ext) {
        Long projectId = asLong(ext, EXT_PROJECT_ID, true);
        Integer deptType = asInteger(ext, EXT_DEPT_TYPE);
        String serviceType = asString(ext, EXT_SERVICE_TYPE);
        Long executeDeptId = asLong(ext, EXT_EXECUTE_DEPT_ID, true);

        if (serviceType == null || serviceType.isBlank()) {
            throw new IllegalArgumentException(
                    "service_launch 工单 ext_json 缺字段 serviceType");
        }

        ServiceItemDO serviceItem = serviceItemMapper
                .selectByProjectIdDeptTypeAndServiceType(projectId, deptType, serviceType);
        if (serviceItem == null) {
            throw new IllegalArgumentException(String.format(
                    "项目 %d 下找不到 部门类型=%s 且 服务类型=%s 的已开始服务项，请先到项目管理里添加并开始该服务项",
                    projectId, deptType, serviceType));
        }

        ServiceLaunchSaveReqVO req = new ServiceLaunchSaveReqVO();
        req.setContractId(serviceItem.getContractId());
        req.setServiceItemId(serviceItem.getId());
        req.setProjectId(projectId);
        req.setExecuteDeptId(executeDeptId);
        req.setIsOutside(asBool(ext, EXT_IS_OUTSIDE));
        req.setDestination(asString(ext, EXT_DESTINATION));
        req.setReason(asString(ext, EXT_REASON));
        req.setPlanStartTime(asDateTime(ext, EXT_PLAN_START_TIME));
        req.setPlanEndTime(asDateTime(ext, EXT_PLAN_END_TIME));
        req.setRemark(event.getRemark());
        return req;
    }

    private Integer asInteger(Map<String, Object> ext, String key) {
        Object raw = ext.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return Integer.valueOf(raw.toString());
    }

    private Long asLong(Map<String, Object> ext, String key, boolean required) {
        Object raw = ext.get(key);
        if (raw == null) {
            if (required) {
                throw new IllegalArgumentException("service_launch 工单 ext_json 缺字段：" + key);
            }
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return Long.valueOf(raw.toString());
    }

    private Boolean asBool(Map<String, Object> ext, String key) {
        Object raw = ext.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        return Boolean.valueOf(raw.toString());
    }

    private String asString(Map<String, Object> ext, String key) {
        Object raw = ext.get(key);
        return raw == null ? null : raw.toString();
    }

    private java.time.LocalDateTime asDateTime(Map<String, Object> ext, String key) {
        Object raw = ext.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) raw;
        }
        // 兼容前端 ISO 8601 字符串
        return java.time.LocalDateTime.parse(raw.toString());
    }

    // 仅在调试时使用：用于把 List<Long> 类型的执行人 ids 序列化成 JSON（兜底，避免依赖业务层格式）
    @SuppressWarnings("unused")
    private static String executorIdsToJson(List<Long> ids) {
        return ids == null ? "[]" : JSONUtil.toJsonStr(ids);
    }

    @SuppressWarnings("unused")
    private static boolean idEquals(Long a, Long b) {
        return Objects.equals(a, b);
    }
}
