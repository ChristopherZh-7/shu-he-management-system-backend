package cn.shuhe.system.module.project.service.listener;

import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import cn.shuhe.system.module.project.service.ServiceLaunchService;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

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
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ProjectRoundService projectRoundService;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @EventListener
    public void onTicketAccepted(TicketAcceptedEvent event) {
        if (!TicketBusinessTypeEnum.SERVICE_LAUNCH.getType().equals(event.getBusinessType())) {
            return;
        }
        Map<String, Object> ext = event.getExtJson();
        ext = ext == null ? Collections.emptyMap() : ext;

        ServiceLaunchSaveReqVO req = buildServiceLaunchReq(event, ext);

        // 1. 创建 service_launch（业务侧自带 isBpmServiceLaunchEnabled 开关；走该开关在 createServiceLaunch
        //    内部决定要不要起 BPM。Listener 不强制改写该开关 —— 由部署方在 application.yml 控制）
        Long launchId = serviceLaunchService.createServiceLaunch(req);
        log.info("【工单接单·服务派遣】已创建 service_launch，launchId={} ticketId={}",
                launchId, event.getTicketId());

        // createServiceLaunch 运行在接单人的登录上下文中，默认会把部门负责人记成服务申请人。
        // 工单模式下申请人应始终是工单发起人，这里在生成履约轮次前纠正审计归属。
        AdminUserRespDTO creator = adminUserApi.getUser(event.getCreatorId());
        ServiceLaunchDO launchUpdate = new ServiceLaunchDO();
        launchUpdate.setId(launchId);
        launchUpdate.setRequestUserId(event.getCreatorId());
        launchUpdate.setRequestDeptId(creator == null ? null : creator.getDeptId());
        serviceLaunchMapper.updateById(launchUpdate);

        // 2. 走「审批通过」路径：写执行人 + 创建 round + 回写 round_id（+ 跨部门成本 / 外出记录）
        Long roundId = serviceLaunchService.handleApproved(launchId, event.getExecutorIds());
        if (roundId == null) {
            throw new IllegalStateException("服务工单接单后未能创建项目执行轮次，ticketId=" + event.getTicketId());
        }

        ProjectRoundDO roundStart = new ProjectRoundDO();
        roundStart.setId(roundId);
        roundStart.setActualStartTime(LocalDateTime.now());
        projectRoundMapper.updateById(roundStart);
        projectRoundService.updateRoundStatus(roundId, 1);
        log.info("【工单接单·服务派遣】已 handleApproved，launchId={} roundId={} executors={}",
                launchId, roundId, event.getExecutorIds());

        // 3. 回写 ticket.business_id 让前端能从工单跳转到 service_launch 详情
        TicketDO ticketUpdate = new TicketDO();
        ticketUpdate.setId(event.getTicketId());
        ticketUpdate.setBusinessId(launchId);
        ticketMapper.updateById(ticketUpdate);
    }

    /**
     * 按工单创建时已校验并固化的 serviceItemId 装配 ServiceLaunchSaveReqVO。
     * 不再使用“项目 + 部门类型 + 服务类型”模糊反查，避免同类型多服务项串单。
     */
    private ServiceLaunchSaveReqVO buildServiceLaunchReq(TicketAcceptedEvent event,
                                                          Map<String, Object> ext) {
        if (event.getServiceItemId() == null) {
            throw new IllegalArgumentException("service_launch 工单缺少 serviceItemId");
        }
        ServiceItemDO serviceItem = serviceItemMapper.selectById(event.getServiceItemId());
        if (serviceItem == null) {
            throw new IllegalArgumentException("服务项不存在：" + event.getServiceItemId());
        }
        if (event.getDeptId() == null) {
            throw new IllegalArgumentException("service_launch 工单缺少负责部门");
        }

        ServiceLaunchSaveReqVO req = new ServiceLaunchSaveReqVO();
        req.setContractId(serviceItem.getContractId());
        req.setServiceItemId(serviceItem.getId());
        req.setProjectId(serviceItem.getProjectId());
        req.setExecuteDeptId(event.getDeptId());
        req.setIsOutside(asBool(ext, EXT_IS_OUTSIDE));
        req.setDestination(asString(ext, EXT_DESTINATION));
        req.setReason(asString(ext, EXT_REASON));
        req.setPlanStartTime(asDateTime(ext, EXT_PLAN_START_TIME));
        req.setPlanEndTime(asDateTime(ext, EXT_PLAN_END_TIME));
        req.setRemark(event.getRemark());
        return req;
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
}
