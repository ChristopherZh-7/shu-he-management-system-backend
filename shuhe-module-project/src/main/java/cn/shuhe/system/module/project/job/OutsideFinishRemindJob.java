package cn.shuhe.system.module.project.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import cn.shuhe.system.module.project.dal.mysql.OutsideMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.OutsideRequestMapper;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 外出完成提醒定时任务
 * 每天早上9点执行，提醒已经外出结束但还未确认完成的人员
 */
@Slf4j
@Component
public class OutsideFinishRemindJob {

    @Resource
    private OutsideRequestMapper outsideRequestMapper;

    @Resource
    private OutsideMemberMapper outsideMemberMapper;

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkApiService dingtalkApiService;

    /**
     * 每天早上9点执行
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        log.info("【外出完成提醒】开始检查需要提醒的外出人员...");
        
        try {
            // 查询状态为"待完成"(status=1)且计划结束时间已过的外出请求
            LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(23, 59, 59);
            List<OutsideRequestDO> pendingRequests = outsideRequestMapper.selectPendingFinishRequests(yesterday);
            
            if (CollUtil.isEmpty(pendingRequests)) {
                log.info("【外出完成提醒】没有需要提醒的外出请求");
                return;
            }
            
            log.info("【外出完成提醒】发现 {} 个需要提醒的外出请求", pendingRequests.size());
            
            // 获取钉钉配置
            List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
            if (CollUtil.isEmpty(configs)) {
                log.warn("【外出完成提醒】没有可用的钉钉配置");
                return;
            }
            DingtalkConfigDO config = configs.get(0);
            
            if (StrUtil.isEmpty(config.getAgentId())) {
                log.warn("【外出完成提醒】钉钉配置缺少agentId");
                return;
            }
            
            String accessToken = dingtalkApiService.getAccessToken(config);
            
            int remindCount = 0;
            // 遍历外出请求，提醒未完成的人员
            for (OutsideRequestDO request : pendingRequests) {
                try {
                    remindCount += remindUnfinishedMembers(request, config, accessToken);
                } catch (Exception e) {
                    log.error("【外出完成提醒】提醒失败：requestId={}", request.getId(), e);
                }
            }
            
            log.info("【外出完成提醒】检查完成，共发送 {} 条提醒", remindCount);
        } catch (Exception e) {
            log.error("【外出完成提醒】执行失败", e);
        }
    }

    /**
     * 提醒未完成确认的外出人员
     * @return 发送的提醒数量
     */
    private int remindUnfinishedMembers(OutsideRequestDO request, DingtalkConfigDO config, String accessToken) {
        // 获取该请求的所有外出人员
        List<OutsideMemberDO> members = outsideMemberMapper.selectListByRequestId(request.getId());
        if (CollUtil.isEmpty(members)) {
            return 0;
        }
        
        int count = 0;
        for (OutsideMemberDO member : members) {
            // 只提醒未完成确认的人员（finishStatus为null或0）
            if (member.getFinishStatus() != null && member.getFinishStatus() > 0) {
                continue; // 已完成，跳过
            }
            
            try {
                if (sendRemind(request, member, config, accessToken)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("【外出完成提醒】发送提醒失败：memberId={}", member.getId(), e);
            }
        }
        
        return count;
    }

    /**
     * 发送完成提醒
     */
    private boolean sendRemind(OutsideRequestDO request, OutsideMemberDO member, 
                               DingtalkConfigDO config, String accessToken) {
        Long userId = member.getUserId();
        if (userId == null) {
            return false;
        }
        
        // 获取用户钉钉ID
        DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(userId, "USER");
        if (mapping == null || StrUtil.isEmpty(mapping.getDingtalkId())) {
            log.debug("【外出完成提醒】用户 {} 没有钉钉映射", userId);
            return false;
        }
        
        // 构建提醒消息
        String title = "📋 外出服务完成提醒";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String planTime = "";
        if (request.getPlanStartTime() != null && request.getPlanEndTime() != null) {
            planTime = request.getPlanStartTime().format(formatter) + " ~ " + request.getPlanEndTime().format(formatter);
        }
        
        String content = String.format(
                "### %s\n\n" +
                "**外出地点：** %s\n\n" +
                "**计划时间：** %s\n\n" +
                "---\n" +
                "您的外出服务已结束，请尽快登录系统确认完成并上传相关附件（如有）。\n\n" +
                "[点击前往确认](http://localhost:5666/project/outside-service/%s?deptType=1)",
                title,
                request.getDestination() != null ? request.getDestination() : "-",
                planTime,
                request.getServiceItemId()
        );
        
        boolean success = dingtalkApiService.sendWorkNotice(
                accessToken,
                config.getAgentId(),
                mapping.getDingtalkId(),
                title,
                content
        );
        
        if (success) {
            log.info("【外出完成提醒】已发送提醒：requestId={}, memberId={}, userName={}", 
                    request.getId(), member.getId(), member.getUserName());
        }
        
        return success;
    }
}
