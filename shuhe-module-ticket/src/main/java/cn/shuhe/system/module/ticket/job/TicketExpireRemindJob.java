package cn.shuhe.system.module.ticket.job;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 工单到期提醒定时任务
 * 每30分钟检查一次，在期望完成时间前3小时发送提醒
 */
@Slf4j
@Component
public class TicketExpireRemindJob {

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkApiService dingtalkApiService;

    /**
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void execute() {
        log.info("【工单到期提醒】开始检查即将到期的工单...");
        
        try {
            // 查询即将到期的工单（期望时间在3小时内，状态为待处理/已分配/处理中）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeHoursLater = now.plusHours(3);
            
            List<TicketDO> expiringTickets = ticketMapper.selectExpiringTickets(now, threeHoursLater);
            
            if (expiringTickets.isEmpty()) {
                log.info("【工单到期提醒】没有即将到期的工单");
                return;
            }
            
            log.info("【工单到期提醒】发现 {} 个即将到期的工单", expiringTickets.size());
            
            // 获取钉钉配置
            List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
            if (configs.isEmpty()) {
                log.warn("【工单到期提醒】没有可用的钉钉配置");
                return;
            }
            DingtalkConfigDO config = configs.get(0);
            String accessToken = dingtalkApiService.getAccessToken(config);
            
            // 发送提醒
            for (TicketDO ticket : expiringTickets) {
                try {
                    sendRemind(ticket, config, accessToken);
                } catch (Exception e) {
                    log.error("【工单到期提醒】发送提醒失败：ticketNo={}", ticket.getTicketNo(), e);
                }
            }
            
            log.info("【工单到期提醒】检查完成");
        } catch (Exception e) {
            log.error("【工单到期提醒】执行失败", e);
        }
    }

    /**
     * 发送到期提醒
     */
    private void sendRemind(TicketDO ticket, DingtalkConfigDO config, String accessToken) {
        // 优先提醒处理人，没有处理人则提醒创建人
        Long userId = ticket.getAssigneeId() != null ? ticket.getAssigneeId() : ticket.getCreatorId();
        if (userId == null) {
            return;
        }
        
        // 获取用户钉钉ID
        DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(userId, "USER");
        if (mapping == null || StrUtil.isEmpty(mapping.getDingtalkId())) {
            log.debug("【工单到期提醒】用户 {} 没有钉钉映射", userId);
            return;
        }
        
        // 构建提醒消息
        String title = "⏰ 工单即将到期";
        String content = String.format(
                "### %s\n\n" +
                "**工单编号：** %s\n\n" +
                "**工单标题：** %s\n\n" +
                "**期望完成：** %s\n\n" +
                "---\n" +
                "请尽快处理！",
                title,
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getExpectTime() != null 
                    ? ticket.getExpectTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : "未设置"
        );
        
        boolean success = dingtalkApiService.sendWorkNotice(
                accessToken,
                config.getAgentId(),
                mapping.getDingtalkId(),
                title,
                content
        );
        
        if (success) {
            log.info("【工单到期提醒】已发送提醒：ticketNo={}, userId={}", ticket.getTicketNo(), userId);
        }
    }
}
