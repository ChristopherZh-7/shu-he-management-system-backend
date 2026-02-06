package cn.shuhe.system.module.project.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.quartz.core.handler.JobHandler;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.service.dingtalkrobot.DingtalkNotificationConfigService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 执行计划截止日期提醒定时任务
 * 
 * 功能说明：
 * 1. 检查所有即将到期的执行计划（remind_time <= 当前时间 且 未提醒 且 未完成）
 * 2. 根据服务项类型确定通知对象：
 *    - 有 executorIds 的：通知执行人
 *    - 驻场服务项（无 executorIds）：通知对应的驻场人员
 * 3. 发送钉钉通知
 * 4. 标记已提醒
 * 
 * 建议配置：
 * - 任务名称：roundDeadlineRemindJob
 * - CRON 表达式：0 0 9 * * ?（每天早上9点执行）
 *
 * @author system
 */
@Slf4j
@Component("roundDeadlineRemindJob")
public class RoundDeadlineRemindJob implements JobHandler {

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectSiteMapper projectSiteMapper;

    @Resource
    private ProjectSiteMemberMapper projectSiteMemberMapper;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private DingtalkNotificationConfigService notificationConfigService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String execute(String param) {
        log.info("[定时任务] 开始执行执行计划截止日期提醒检查...");
        long startTime = System.currentTimeMillis();
        
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        
        try {
            // 1. 查询所有需要提醒的执行计划
            LocalDateTime now = LocalDateTime.now();
            List<ProjectRoundDO> roundsToRemind = projectRoundMapper.selectRoundsNeedRemind(now);
            
            if (CollUtil.isEmpty(roundsToRemind)) {
                log.info("[定时任务] 没有需要提醒的执行计划");
                return "没有需要提醒的执行计划";
            }
            
            totalCount = roundsToRemind.size();
            log.info("[定时任务] 找到 {} 个需要提醒的执行计划", totalCount);
            
            // 2. 预加载服务项信息
            Set<Long> serviceItemIds = roundsToRemind.stream()
                    .map(ProjectRoundDO::getServiceItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, ServiceItemDO> serviceItemMap = new HashMap<>();
            if (CollUtil.isNotEmpty(serviceItemIds)) {
                List<ServiceItemDO> serviceItems = serviceItemMapper.selectBatchIds(serviceItemIds);
                serviceItemMap = serviceItems.stream()
                        .collect(Collectors.toMap(ServiceItemDO::getId, s -> s, (a, b) -> a));
            }
            
            // 3. 逐个处理
            for (ProjectRoundDO round : roundsToRemind) {
                try {
                    processRoundRemind(round, serviceItemMap);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[定时任务] 处理执行计划提醒失败，roundId={}", round.getId(), e);
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            String result = String.format("执行完成，总计=%d，成功=%d，失败=%d，耗时=%dms", 
                    totalCount, successCount, failCount, elapsed);
            log.info("[定时任务] {}", result);
            
            return result;
        } catch (Exception e) {
            log.error("[定时任务] 执行计划截止日期提醒任务执行失败", e);
            return "任务执行失败: " + e.getMessage();
        }
    }

    /**
     * 处理单个执行计划的提醒
     */
    private void processRoundRemind(ProjectRoundDO round, Map<Long, ServiceItemDO> serviceItemMap) {
        Long serviceItemId = round.getServiceItemId();
        ServiceItemDO serviceItem = serviceItemMap.get(serviceItemId);
        
        if (serviceItem == null) {
            log.warn("[定时任务] 执行计划关联的服务项不存在，roundId={}，serviceItemId={}", 
                    round.getId(), serviceItemId);
            // 标记已提醒，避免重复处理
            markAsReminded(round.getId());
            return;
        }
        
        // 1. 确定通知对象
        List<Long> notifyUserIds = resolveNotifyUserIds(round, serviceItem);
        
        if (CollUtil.isEmpty(notifyUserIds)) {
            log.warn("[定时任务] 没有找到需要通知的用户，roundId={}，serviceItemId={}", 
                    round.getId(), serviceItemId);
            // 标记已提醒
            markAsReminded(round.getId());
            return;
        }
        
        // 2. 计算剩余天数
        long remainingDays = 0;
        if (round.getDeadline() != null) {
            remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), round.getDeadline().toLocalDate());
            if (remainingDays < 0) {
                remainingDays = 0;
            }
        }
        
        // 3. 构建通知变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceItemName", serviceItem.getName());
        variables.put("roundName", round.getName());
        variables.put("deadline", round.getDeadline() != null ? 
                round.getDeadline().format(DATE_FORMATTER) : "未设置");
        variables.put("remainingDays", remainingDays);
        variables.put("customerName", serviceItem.getCustomerName() != null ? 
                serviceItem.getCustomerName() : "-");
        variables.put("serviceType", serviceItem.getServiceType());
        
        // 4. 发送通知
        // 通过负责人ID触发通知（将第一个用户作为负责人）
        Long ownerUserId = notifyUserIds.get(0);
        
        notificationConfigService.triggerNotification(
                "round_deadline_remind",
                "project",
                round.getId(),
                "ROUND-" + round.getId(),
                variables,
                ownerUserId,
                null
        );
        
        log.info("[定时任务] 执行计划提醒通知已发送，roundId={}，serviceItemName={}，notifyUsers={}", 
                round.getId(), serviceItem.getName(), notifyUserIds);
        
        // 5. 标记已提醒
        markAsReminded(round.getId());
    }

    /**
     * 解析需要通知的用户ID列表
     */
    private List<Long> resolveNotifyUserIds(ProjectRoundDO round, ServiceItemDO serviceItem) {
        List<Long> userIds = new ArrayList<>();
        
        // 1. 优先使用 Round 中的 executorIds
        if (round.getExecutorIds() != null && !round.getExecutorIds().isEmpty()) {
            try {
                List<Long> executorIds = JSONUtil.toList(round.getExecutorIds(), Long.class);
                if (CollUtil.isNotEmpty(executorIds)) {
                    userIds.addAll(executorIds);
                    return userIds;
                }
            } catch (Exception e) {
                log.warn("[定时任务] 解析 executorIds 失败，roundId={}", round.getId(), e);
            }
        }
        
        // 2. 判断是否为驻场服务项，如果是则通知驻场人员
        if (isOnsiteServiceItem(serviceItem)) {
            List<Long> onsiteUserIds = getOnsiteMemberUserIds(serviceItem);
            if (CollUtil.isNotEmpty(onsiteUserIds)) {
                userIds.addAll(onsiteUserIds);
            }
        }
        
        return userIds;
    }

    /**
     * 判断是否为驻场服务项
     */
    private boolean isOnsiteServiceItem(ServiceItemDO serviceItem) {
        Integer deptType = serviceItem.getDeptType();
        if (deptType == null) {
            return false;
        }
        
        if (deptType == 1 || deptType == 3) {
            // 安全服务 / 数据安全：serviceMode=1 表示驻场
            Integer serviceMode = serviceItem.getServiceMode();
            return serviceMode != null && serviceMode == ServiceItemDO.SERVICE_MODE_ONSITE;
        } else if (deptType == 2) {
            // 安全运营：serviceMemberType=1 表示驻场人员服务项
            Integer serviceMemberType = serviceItem.getServiceMemberType();
            return serviceMemberType != null && 
                    serviceMemberType == ServiceItemDO.SERVICE_MEMBER_TYPE_ONSITE;
        }
        
        return false;
    }

    /**
     * 获取驻场人员的用户ID列表
     */
    private List<Long> getOnsiteMemberUserIds(ServiceItemDO serviceItem) {
        List<Long> userIds = new ArrayList<>();
        
        Long projectId = serviceItem.getProjectId();
        if (projectId == null) {
            return userIds;
        }
        
        Integer deptType = serviceItem.getDeptType();
        
        // 1. 查找项目对应部门类型的驻场点
        List<ProjectSiteDO> sites = projectSiteMapper.selectListByProjectIdAndDeptType(projectId, deptType);
        if (CollUtil.isEmpty(sites)) {
            return userIds;
        }
        
        // 2. 获取驻场点的驻场人员（memberType=2）
        List<Long> siteIds = sites.stream().map(ProjectSiteDO::getId).collect(Collectors.toList());
        List<ProjectSiteMemberDO> members = projectSiteMemberMapper.selectListBySiteIds(siteIds);
        
        // 3. 过滤出驻场人员（memberType=2）且在岗状态（status=1）
        for (ProjectSiteMemberDO member : members) {
            if (member.getMemberType() != null && 
                    member.getMemberType() == ProjectSiteMemberDO.MEMBER_TYPE_ONSITE &&
                    member.getStatus() != null && 
                    member.getStatus() == ProjectSiteMemberDO.STATUS_ACTIVE &&
                    member.getUserId() != null) {
                userIds.add(member.getUserId());
            }
        }
        
        return userIds;
    }

    /**
     * 标记执行计划已提醒
     */
    private void markAsReminded(Long roundId) {
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(roundId);
        updateObj.setReminded(true);
        projectRoundMapper.updateById(updateObj);
    }

}
