package cn.shuhe.system.module.system.service.dashboard;

import cn.shuhe.system.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.api.dashboard.DashboardBpmApi;
import cn.shuhe.system.module.system.api.dashboard.DashboardCrmApi;
import cn.shuhe.system.module.system.api.dashboard.DashboardProjectApi;
import cn.shuhe.system.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO.*;
import cn.shuhe.system.module.system.dal.dataobject.logger.OperateLogDO;
import cn.shuhe.system.module.system.service.logger.OperateLogService;
import cn.shuhe.system.module.system.service.permission.PermissionService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仪表板统计 Service 实现类
 * 
 * 权限说明：
 * - 管理员（super_admin 角色）：可查看全局数据
 * - 部门负责人：可查看本部门及下属部门数据
 * - 普通用户：只能查看自己相关的数据
 * 
 * TODO: 后续需要对接各模块的真实数据
 * - 项目数据：project 模块
 * - 合同数据：crm 模块
 * - 客户数据：crm 模块
 * - 任务数据：bpm 模块
 * - 财务数据：crm 回款/receivable 模块
 */
@Slf4j
@Service
public class DashboardStatisticsServiceImpl implements DashboardStatisticsService {

    @Resource
    private PermissionService permissionService;

    @Resource
    private AdminUserService adminUserService;

    @Autowired(required = false)
    private DashboardProjectApi dashboardProjectApi;

    @Autowired(required = false)
    private DashboardBpmApi dashboardBpmApi;

    @Autowired(required = false)
    private DashboardCrmApi dashboardCrmApi;

    @Resource
    private OperateLogService operateLogService;

    @Resource
    private AdminUserApi adminUserApi;

    /**
     * 管理员角色标识
     */
    private static final String SUPER_ADMIN_ROLE = "super_admin";

    @Override
    public boolean isAdmin(Long userId) {
        return permissionService.hasAnyRoles(userId, SUPER_ADMIN_ROLE);
    }

    /**
     * 获取用户的数据权限范围
     */
    private DeptDataPermissionRespDTO getUserDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }

    /**
     * 获取用户可见的部门ID集合
     */
    private Set<Long> getVisibleDeptIds(Long userId) {
        DeptDataPermissionRespDTO permission = getUserDataPermission(userId);
        if (permission == null) {
            return Set.of();
        }
        // 如果是全部数据权限，返回 null 表示不限制
        if (Boolean.TRUE.equals(permission.getAll())) {
            return null;
        }
        return permission.getDeptIds();
    }

    @Override
    public DashboardStatisticsRespVO getStatistics(Long userId, String pageType) {
        DashboardStatisticsRespVO.DashboardStatisticsRespVOBuilder builder = DashboardStatisticsRespVO.builder();

        boolean admin = isAdmin(userId);
        log.debug("获取仪表板统计数据, userId={}, isAdmin={}, pageType={}", userId, admin, pageType);

        // 统计卡片数据
        builder.projectStats(getProjectStats(userId));
        builder.contractStats(getContractStats(userId));
        builder.customerStats(getCustomerStats(userId));
        builder.taskStats(getTaskStats(userId));
        builder.revenueStats(getRevenueStats(userId));

        // 图表数据（管理员看全局，普通用户看个人/部门）
        builder.trendData(getTrendData(userId, admin));
        builder.projectDistribution(getProjectDistribution(userId, admin));
        builder.taskDistribution(getTaskDistribution(userId, admin));
        builder.deptRanking(getDeptRanking(userId, admin));

        // 列表数据（待办来自 BPM 真实数据）
        builder.todoList(getTodoList(userId));
        builder.recentActivities(getRecentActivities(userId, admin));
        builder.contractReminders(getContractReminders(userId, admin));

        return builder.build();
    }

    @Override
    public ProjectStats getProjectStats(Long userId) {
        if (dashboardProjectApi != null) {
            return dashboardProjectApi.getProjectStats(userId);
        }
        boolean admin = isAdmin(userId);
        if (admin) {
            return ProjectStats.builder()
                    .activeCount(12)
                    .totalCount(86)
                    .monthlyNewCount(3)
                    .completedCount(68)
                    .build();
        } else {
            return ProjectStats.builder()
                    .activeCount(3)
                    .totalCount(15)
                    .monthlyNewCount(1)
                    .completedCount(10)
                    .build();
        }
    }

    @Override
    public ContractStats getContractStats(Long userId) {
        if (dashboardCrmApi != null) {
            ContractStats real = dashboardCrmApi.getContractStats(userId);
            if (real != null) {
                return real;
            }
        }
        boolean admin = isAdmin(userId);
        if (admin) {
            return ContractStats.builder()
                    .activeCount(28)
                    .totalCount(156)
                    .pendingAuditCount(5)
                    .expiringCount(3)
                    .totalAmount(new BigDecimal("8560000"))
                    .build();
        } else {
            // 普通用户看自己负责的合同
            return ContractStats.builder()
                    .activeCount(5)
                    .totalCount(18)
                    .pendingAuditCount(1)
                    .expiringCount(1)
                    .totalAmount(new BigDecimal("1250000"))
                    .build();
        }
    }

    @Override
    public CustomerStats getCustomerStats(Long userId) {
        if (dashboardCrmApi != null) {
            CustomerStats real = dashboardCrmApi.getCustomerStats(userId);
            if (real != null) {
                return real;
            }
        }
        boolean admin = isAdmin(userId);
        if (admin) {
            return CustomerStats.builder()
                    .totalCount(325)
                    .todayContactCount(8)
                    .followUpCount(15)
                    .monthlyNewCount(12)
                    .build();
        } else {
            // 普通用户看自己负责的客户
            return CustomerStats.builder()
                    .totalCount(28)
                    .todayContactCount(2)
                    .followUpCount(5)
                    .monthlyNewCount(3)
                    .build();
        }
    }

    @Override
    public TaskStats getTaskStats(Long userId) {
        if (dashboardBpmApi != null) {
            TaskStats real = dashboardBpmApi.getTaskStats(userId);
            if (real != null) {
                return real;
            }
        }
        return TaskStats.builder()
                .todoCount(23)
                .todayDoneCount(5)
                .weeklyDoneCount(32)
                .overdueCount(2)
                .build();
    }

    @Override
    public RevenueStats getRevenueStats(Long userId) {
        if (dashboardCrmApi != null) {
            RevenueStats real = dashboardCrmApi.getRevenueStats(userId);
            if (real != null) {
                return real;
            }
        }
        boolean admin = isAdmin(userId);
        if (admin) {
            BigDecimal revenue = new BigDecimal("1256000");
            BigDecimal cost = new BigDecimal("680000");
            return RevenueStats.builder()
                    .monthlyRevenue(revenue)
                    .monthlyCost(cost)
                    .monthlyProfit(revenue.subtract(cost))
                    .growthRate(new BigDecimal("12.5"))
                    .yearlyRevenue(new BigDecimal("12800000"))
                    .build();
        } else {
            // 普通用户不显示全局收入数据，或只显示个人相关
            BigDecimal revenue = new BigDecimal("185000");
            BigDecimal cost = new BigDecimal("95000");
            return RevenueStats.builder()
                    .monthlyRevenue(revenue)
                    .monthlyCost(cost)
                    .monthlyProfit(revenue.subtract(cost))
                    .growthRate(new BigDecimal("8.5"))
                    .yearlyRevenue(new BigDecimal("1560000"))
                    .build();
        }
    }

    /**
     * 获取经营趋势数据（最近12个月）
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<TrendData> getTrendData(Long userId, boolean isAdmin) {
        if (dashboardCrmApi != null) {
            List<TrendData> real = dashboardCrmApi.getTrendData(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }
        List<TrendData> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        // 使用「年+月」避免跨年时出现重复「2月」等导致 X 轴与 tooltip 错位
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月");
        // 管理员看全公司，普通用户看个人/部门相关
        int[] revenues;
        int[] costs;
        
        if (isAdmin) {
            revenues = new int[]{120, 132, 101, 134, 90, 230, 210, 182, 191, 234, 290, 330};
            costs = new int[]{80, 92, 71, 94, 60, 150, 130, 112, 121, 154, 180, 200};
        } else {
            // 普通用户看个人业绩相关
            revenues = new int[]{15, 18, 12, 20, 8, 25, 22, 19, 21, 28, 32, 35};
            costs = new int[]{8, 10, 7, 12, 5, 14, 12, 10, 11, 15, 18, 20};
        }

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            int idx = 11 - i;
            BigDecimal revenue = BigDecimal.valueOf(revenues[idx] * 10000L);
            BigDecimal cost = BigDecimal.valueOf(costs[idx] * 10000L);
            list.add(TrendData.builder()
                    .month(month.format(formatter))
                    .revenue(revenue)
                    .cost(cost)
                    .profit(revenue.subtract(cost))
                    .build());
        }
        return list;
    }

    /**
     * 获取项目状态分布
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<PieChartData> getProjectDistribution(Long userId, boolean isAdmin) {
        List<PieChartData> list = new ArrayList<>();
        // TODO: 从 project 模块获取真实数据
        if (isAdmin) {
            list.add(PieChartData.builder().name("进行中").value(12).color("#5470c6").build());
            list.add(PieChartData.builder().name("已完成").value(68).color("#91cc75").build());
            list.add(PieChartData.builder().name("已暂停").value(4).color("#fac858").build());
            list.add(PieChartData.builder().name("已取消").value(2).color("#ee6666").build());
        } else {
            // 普通用户看自己参与的项目
            list.add(PieChartData.builder().name("进行中").value(3).color("#5470c6").build());
            list.add(PieChartData.builder().name("已完成").value(10).color("#91cc75").build());
            list.add(PieChartData.builder().name("已暂停").value(1).color("#fac858").build());
            list.add(PieChartData.builder().name("已取消").value(1).color("#ee6666").build());
        }
        return list;
    }

    /**
     * 获取任务状态分布
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<PieChartData> getTaskDistribution(Long userId, boolean isAdmin) {
        if (dashboardBpmApi != null) {
            List<PieChartData> real = dashboardBpmApi.getTaskDistribution(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }
        List<PieChartData> list = new ArrayList<>();
        if (isAdmin) {
            list.add(PieChartData.builder().name("待处理").value(23).color("#5470c6").build());
            list.add(PieChartData.builder().name("处理中").value(15).color("#91cc75").build());
            list.add(PieChartData.builder().name("已完成").value(89).color("#73c0de").build());
            list.add(PieChartData.builder().name("已驳回").value(8).color("#ee6666").build());
        } else {
            // 普通用户看自己的任务
            list.add(PieChartData.builder().name("待处理").value(5).color("#5470c6").build());
            list.add(PieChartData.builder().name("处理中").value(3).color("#91cc75").build());
            list.add(PieChartData.builder().name("已完成").value(18).color("#73c0de").build());
            list.add(PieChartData.builder().name("已驳回").value(2).color("#ee6666").build());
        }
        return list;
    }

    /**
     * 获取部门排行（只有管理员可见）
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<RankData> getDeptRanking(Long userId, boolean isAdmin) {
        if (dashboardCrmApi != null) {
            List<RankData> real = dashboardCrmApi.getDeptRanking(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }
        List<RankData> list = new ArrayList<>();
        if (isAdmin) {
            list.add(RankData.builder().rank(1).deptName("渗透测试部").amount(new BigDecimal("3250000")).completionRate(new BigDecimal("125.5")).build());
            list.add(RankData.builder().rank(2).deptName("安全运营部").amount(new BigDecimal("2860000")).completionRate(new BigDecimal("110.2")).build());
            list.add(RankData.builder().rank(3).deptName("安全服务部").amount(new BigDecimal("2150000")).completionRate(new BigDecimal("95.8")).build());
            list.add(RankData.builder().rank(4).deptName("安全集成部").amount(new BigDecimal("1680000")).completionRate(new BigDecimal("88.4")).build());
            list.add(RankData.builder().rank(5).deptName("研发部").amount(new BigDecimal("920000")).completionRate(new BigDecimal("76.5")).build());
        } else {
            list.add(RankData.builder().rank(1).deptName("我的业绩").amount(new BigDecimal("350000")).completionRate(new BigDecimal("105.5")).build());
        }
        return list;
    }

    /**
     * 获取待办任务列表（来自 BPM 真实待办，最多 20 条）
     */
    private List<TodoItem> getTodoList(Long userId) {
        if (dashboardBpmApi != null) {
            List<TodoItem> real = dashboardBpmApi.getTodoList(userId, 20);
            if (real != null) {
                return real;
            }
        }
        List<TodoItem> list = new ArrayList<>();
        list.add(TodoItem.builder()
                .id("1")
                .title("合同审批 - XX科技有限公司")
                .processName("合同审批")
                .createTimeDesc("2小时前")
                .status("urgent")
                .build());
        list.add(TodoItem.builder()
                .id("2")
                .title("外出申请 - 张三")
                .processName("外出审批")
                .createTimeDesc("3小时前")
                .status("pending")
                .build());
        list.add(TodoItem.builder()
                .id("3")
                .title("服务发起 - 渗透测试")
                .processName("服务发起")
                .createTimeDesc("今天 10:30")
                .status("pending")
                .build());
        list.add(TodoItem.builder()
                .id("4")
                .title("请假申请 - 李四")
                .processName("请假审批")
                .createTimeDesc("昨天 16:20")
                .status("pending")
                .build());
        list.add(TodoItem.builder()
                .id("5")
                .title("费用报销 - 差旅费")
                .processName("费用审批")
                .createTimeDesc("昨天 09:15")
                .status("pending")
                .build());
        return list;
    }

    /**
     * 获取最近活动（从操作日志获取真实数据）
     * 管理员看全公司活动，普通用户只看自己的操作记录
     */
    private List<ActivityItem> getRecentActivities(Long userId, boolean isAdmin) {
        OperateLogPageReqDTO reqDTO = new OperateLogPageReqDTO();
        reqDTO.setPageNo(1);
        reqDTO.setPageSize(10);
        if (!isAdmin) {
            reqDTO.setUserId(userId);
        }
        PageResult<OperateLogDO> pageResult = operateLogService.getOperateLogPage(reqDTO);
        List<OperateLogDO> logs = pageResult.getList();
        if (logs == null || logs.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = logs.stream()
                .map(OperateLogDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Map.of() : adminUserApi.getUserMap(userIds);
        List<ActivityItem> list = new ArrayList<>();
        for (OperateLogDO log : logs) {
            String operator = "未知";
            if (log.getUserId() != null && userMap != null) {
                AdminUserRespDTO user = userMap.get(log.getUserId());
                if (user != null && user.getNickname() != null) {
                    operator = user.getNickname();
                }
            }
            String type = log.getType() != null && !log.getType().isBlank() ? log.getType() : "task";
            String title = log.getSubType() != null && !log.getSubType().isBlank() ? log.getSubType() : "操作";
            String description = log.getAction() != null ? log.getAction() : "";
            list.add(ActivityItem.builder()
                    .id(log.getId())
                    .type(type)
                    .title(title)
                    .description(description)
                    .operator(operator)
                    .timeDesc(formatRelativeTime(log.getCreateTime()))
                    .refId(log.getBizId())
                    .build());
        }
        return list;
    }

    private static String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(time, now);
        long hours = ChronoUnit.HOURS.between(time, now);
        long days = ChronoUnit.DAYS.between(time, now);
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        if (hours < 24) return hours + "小时前";
        if (days == 0) return "今天 " + time.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (days == 1) return "昨天 " + time.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (days < 7) return days + "天前";
        return time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
    }

    /**
     * 获取合同提醒
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<ContractRemindItem> getContractReminders(Long userId, boolean isAdmin) {
        if (dashboardCrmApi != null) {
            List<ContractRemindItem> real = dashboardCrmApi.getContractReminders(userId, isAdmin);
            if (real != null) {
                return real;
            }
        }
        List<ContractRemindItem> list = new ArrayList<>();
        if (isAdmin) {
            // 管理员看所有即将到期合同
            list.add(ContractRemindItem.builder()
                    .id(1L)
                    .contractNo("HT-2026-001")
                    .contractName("XX银行安全服务合同")
                    .customerName("XX银行")
                    .endDate("2026-02-15")
                    .remainingDays(17)
                    .amount(new BigDecimal("680000"))
                    .build());
            list.add(ContractRemindItem.builder()
                    .id(2L)
                    .contractNo("HT-2026-008")
                    .contractName("XX集团安全运营合同")
                    .customerName("XX集团")
                    .endDate("2026-02-28")
                    .remainingDays(30)
                    .amount(new BigDecimal("1200000"))
                    .build());
            list.add(ContractRemindItem.builder()
                    .id(3L)
                    .contractNo("HT-2025-156")
                    .contractName("XX医院等保服务合同")
                    .customerName("XX医院")
                    .endDate("2026-03-15")
                    .remainingDays(45)
                    .amount(new BigDecimal("350000"))
                    .build());
        } else {
            // 普通用户只看自己负责的合同
            list.add(ContractRemindItem.builder()
                    .id(1L)
                    .contractNo("HT-2026-008")
                    .contractName("我负责的合同")
                    .customerName("XX客户")
                    .endDate("2026-02-28")
                    .remainingDays(30)
                    .amount(new BigDecimal("350000"))
                    .build());
        }
        return list;
    }

}
