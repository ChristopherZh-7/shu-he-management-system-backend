package cn.shuhe.system.module.finance.service.dashboard;

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
import cn.shuhe.system.module.finance.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.finance.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.dal.dataobject.logger.OperateLogDO;
import cn.shuhe.system.module.finance.service.cost.BusinessAnalysisCacheService;
import cn.shuhe.system.module.finance.service.cost.BusinessAnalysisService;
import cn.shuhe.system.module.system.service.logger.OperateLogService;
import cn.shuhe.system.module.system.service.permission.PermissionService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
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

    @Autowired(required = false)
    private BusinessAnalysisService businessAnalysisService;

    @Autowired(required = false)
    private BusinessAnalysisCacheService businessAnalysisCacheService;

    /**
     * 管理员角色标识
     */
    private static final String SUPER_ADMIN_ROLE = "super_admin";

    /**
     * TTL-wrapped executor: 自动将 TransmittableThreadLocal（包括 SecurityContext）
     * 从提交线程传播到 ForkJoinPool 工作线程。
     */
    private static final Executor TTL_EXECUTOR = TtlExecutors.getTtlExecutor(ForkJoinPool.commonPool());

    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, TTL_EXECUTOR);
    }

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
        boolean admin = isAdmin(userId);
        log.debug("获取仪表板统计数据, userId={}, isAdmin={}, pageType={}", userId, admin, pageType);

        long startTime = System.currentTimeMillis();

        // 【性能优化】所有子查询并行执行，总耗时从「各查询之和」降为「最慢的单个查询」
        CompletableFuture<ProjectStats> projectStatsFuture =
                runAsync(() -> getProjectStats(userId));
        CompletableFuture<ContractStats> contractStatsFuture =
                runAsync(() -> getContractStats(userId));
        CompletableFuture<CustomerStats> customerStatsFuture =
                runAsync(() -> getCustomerStats(userId));
        CompletableFuture<TaskStats> taskStatsFuture =
                runAsync(() -> getTaskStats(userId));
        CompletableFuture<RevenueStats> revenueStatsFuture =
                runAsync(() -> getRevenueStats(userId));
        CompletableFuture<List<TrendData>> trendDataFuture =
                runAsync(() -> getTrendData(userId, admin));
        CompletableFuture<List<PieChartData>> projectDistFuture =
                runAsync(() -> getProjectDistribution(userId, admin));
        CompletableFuture<List<PieChartData>> taskDistFuture =
                runAsync(() -> getTaskDistribution(userId, admin));
        CompletableFuture<List<RankData>> deptRankingFuture =
                runAsync(() -> getDeptRanking(userId, admin));
        CompletableFuture<List<TodoItem>> todoListFuture =
                runAsync(() -> getTodoList(userId));
        CompletableFuture<List<ActivityItem>> recentActivitiesFuture =
                runAsync(() -> getRecentActivities(userId, admin));
        CompletableFuture<List<ContractRemindItem>> contractRemindersFuture =
                runAsync(() -> getContractReminders(userId, admin));

        CompletableFuture.allOf(
                projectStatsFuture, contractStatsFuture, customerStatsFuture, taskStatsFuture,
                revenueStatsFuture, trendDataFuture, projectDistFuture, taskDistFuture,
                deptRankingFuture, todoListFuture, recentActivitiesFuture, contractRemindersFuture
        ).join();

        log.info("仪表板统计数据并行查询完成，总耗时={}ms", System.currentTimeMillis() - startTime);

        return DashboardStatisticsRespVO.builder()
                .projectStats(projectStatsFuture.join())
                .contractStats(contractStatsFuture.join())
                .customerStats(customerStatsFuture.join())
                .taskStats(taskStatsFuture.join())
                .revenueStats(revenueStatsFuture.join())
                .trendData(trendDataFuture.join())
                .projectDistribution(projectDistFuture.join())
                .taskDistribution(taskDistFuture.join())
                .deptRanking(deptRankingFuture.join())
                .todoList(todoListFuture.join())
                .recentActivities(recentActivitiesFuture.join())
                .contractReminders(contractRemindersFuture.join())
                .build();
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
        return getRevenueStats(userId, null, null);
    }

    @Override
    public RevenueStats getRevenueStats(Long userId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();

        if (businessAnalysisService != null) {
            try {
                if (month != null && month >= 1 && month <= 12) {
                    return getMonthRevenueStats(userId, targetYear, month);
                } else {
                    return getYtdRevenueStats(userId, targetYear, now);
                }
            } catch (Exception e) {
                log.warn("从经营分析服务获取利润数据失败，使用降级方案", e);
            }
        }

        if (dashboardCrmApi != null) {
            RevenueStats real = dashboardCrmApi.getRevenueStats(userId);
            if (real != null) {
                return real;
            }
        }

        return RevenueStats.builder()
                .monthlyRevenue(BigDecimal.ZERO)
                .monthlyCost(BigDecimal.ZERO)
                .monthlyProfit(BigDecimal.ZERO)
                .growthRate(BigDecimal.ZERO)
                .yearlyRevenue(BigDecimal.ZERO)
                .weeklyRevenue(BigDecimal.ZERO)
                .build();
    }

    /**
     * 获取单月收入统计：cumulative(month) - cumulative(month-1)
     */
    private RevenueStats getMonthRevenueStats(Long userId, int year, int month) {
        LocalDate monthEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        LocalDate cutoff = monthEnd.isAfter(LocalDate.now()) ? LocalDate.now() : monthEnd;

        BigDecimal contractYearlyRevenue = getContractYearlyTotal(year);

        BusinessAnalysisReqVO curReq = new BusinessAnalysisReqVO();
        curReq.setYear(year);
        curReq.setCutoffDate(cutoff);
        curReq.setLevel(1);
        curReq.setIncludeEmployees(false);

        BusinessAnalysisRespVO curData = businessAnalysisService.getBusinessAnalysis(curReq, userId);
        BigDecimal curRevenue = BigDecimal.ZERO, curCost = BigDecimal.ZERO, curProfit = BigDecimal.ZERO;
        if (curData != null && curData.getTotal() != null) {
            curRevenue = orZero(curData.getTotal().getTotalIncome());
            curCost = orZero(curData.getTotal().getTotalExpense());
            curProfit = orZero(curData.getTotal().getNetProfit());
        }

        BigDecimal prevRevenue = BigDecimal.ZERO, prevCost = BigDecimal.ZERO, prevProfit = BigDecimal.ZERO;
        if (month > 1) {
            LocalDate prevEnd = LocalDate.of(year, month - 1, 1).plusMonths(1).minusDays(1);
            BusinessAnalysisReqVO prevReq = new BusinessAnalysisReqVO();
            prevReq.setYear(year);
            prevReq.setCutoffDate(prevEnd);
            prevReq.setLevel(1);
            prevReq.setIncludeEmployees(false);
            BusinessAnalysisRespVO prevData = businessAnalysisService.getBusinessAnalysis(prevReq, userId);
            if (prevData != null && prevData.getTotal() != null) {
                prevRevenue = orZero(prevData.getTotal().getTotalIncome());
                prevCost = orZero(prevData.getTotal().getTotalExpense());
                prevProfit = orZero(prevData.getTotal().getNetProfit());
            }
        }

        BigDecimal monthRevenue = curRevenue.subtract(prevRevenue);
        BigDecimal monthCost = curCost.subtract(prevCost);
        BigDecimal monthProfit = curProfit.subtract(prevProfit);

        BigDecimal growthRate = computeGrowth(userId, year, month, monthProfit);

        BigDecimal weeklyRevenue = computeWeeklyRevenue(userId);
        return RevenueStats.builder()
                .monthlyRevenue(monthRevenue)
                .monthlyCost(monthCost)
                .monthlyProfit(monthProfit)
                .growthRate(growthRate)
                .yearlyRevenue(curRevenue)
                .weeklyRevenue(weeklyRevenue)
                .contractYearlyRevenue(contractYearlyRevenue)
                .build();
    }

    /**
     * 获取 YTD 收入统计
     */
    private RevenueStats getYtdRevenueStats(Long userId, int targetYear, LocalDate now) {
        LocalDate cutoff = (targetYear == now.getYear()) ? now : LocalDate.of(targetYear, 12, 31);

        BigDecimal contractYearlyRevenue = getContractYearlyTotal(targetYear);

        BusinessAnalysisReqVO req = new BusinessAnalysisReqVO();
        req.setYear(targetYear);
        req.setCutoffDate(cutoff);
        req.setLevel(1);
        req.setIncludeEmployees(false);
        BusinessAnalysisRespVO data = businessAnalysisService.getBusinessAnalysis(req, userId);

        if (data != null && data.getTotal() != null) {
            BigDecimal yearlyRevenue = orZero(data.getTotal().getTotalIncome());
            BigDecimal yearlyCost = orZero(data.getTotal().getTotalExpense());
            BigDecimal yearlyProfit = orZero(data.getTotal().getNetProfit());

            int monthsPassed = Math.max(cutoff.getMonthValue(), 1);
            BigDecimal avgRevenue = yearlyRevenue.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal avgCost = yearlyCost.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal avgProfit = yearlyProfit.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);

            BigDecimal growthRate = BigDecimal.ZERO;
            try {
                BusinessAnalysisReqVO lastYearReq = new BusinessAnalysisReqVO();
                lastYearReq.setYear(targetYear - 1);
                lastYearReq.setCutoffDate(cutoff.minusYears(1));
                lastYearReq.setLevel(1);
                lastYearReq.setIncludeEmployees(false);
                BusinessAnalysisRespVO lastYearData = businessAnalysisService.getBusinessAnalysis(lastYearReq, userId);
                if (lastYearData != null && lastYearData.getTotal() != null) {
                    BigDecimal lastYearProfit = orZero(lastYearData.getTotal().getNetProfit());
                    if (lastYearProfit.compareTo(BigDecimal.ZERO) > 0) {
                        growthRate = yearlyProfit.subtract(lastYearProfit)
                                .multiply(new BigDecimal("100"))
                                .divide(lastYearProfit, 1, java.math.RoundingMode.HALF_UP);
                    } else if (yearlyProfit.compareTo(BigDecimal.ZERO) > 0) {
                        growthRate = new BigDecimal("100");
                    }
                }
            } catch (Exception e) {
                log.warn("计算同比增长率失败", e);
            }

            BigDecimal weeklyRevenue = computeWeeklyRevenue(userId);
            return RevenueStats.builder()
                    .monthlyRevenue(avgRevenue)
                    .monthlyCost(avgCost)
                    .monthlyProfit(avgProfit)
                    .growthRate(growthRate)
                    .yearlyRevenue(yearlyRevenue)
                    .weeklyRevenue(weeklyRevenue)
                    .contractYearlyRevenue(contractYearlyRevenue)
                    .build();
        }

        return RevenueStats.builder()
                .monthlyRevenue(BigDecimal.ZERO).monthlyCost(BigDecimal.ZERO)
                .monthlyProfit(BigDecimal.ZERO).growthRate(BigDecimal.ZERO)
                .yearlyRevenue(BigDecimal.ZERO).weeklyRevenue(BigDecimal.ZERO)
                .contractYearlyRevenue(contractYearlyRevenue).build();
    }

    private BigDecimal getContractYearlyTotal(int year) {
        if (dashboardCrmApi != null) {
            BigDecimal total = dashboardCrmApi.getContractYearlyTotal(null, year);
            if (total != null) return total;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal computeGrowth(Long userId, int year, int month, BigDecimal currentProfit) {
        try {
            LocalDate lastYearEnd = LocalDate.of(year - 1, month, 1).plusMonths(1).minusDays(1);
            BusinessAnalysisReqVO lastReq = new BusinessAnalysisReqVO();
            lastReq.setYear(year - 1);
            lastReq.setCutoffDate(lastYearEnd);
            lastReq.setLevel(1);
            lastReq.setIncludeEmployees(false);
            BusinessAnalysisRespVO lastData = businessAnalysisService.getBusinessAnalysis(lastReq, userId);

            BigDecimal lastCumProfit = BigDecimal.ZERO;
            if (lastData != null && lastData.getTotal() != null) {
                lastCumProfit = orZero(lastData.getTotal().getNetProfit());
            }
            BigDecimal lastPrevProfit = BigDecimal.ZERO;
            if (month > 1) {
                LocalDate lastPrevEnd = LocalDate.of(year - 1, month - 1, 1).plusMonths(1).minusDays(1);
                BusinessAnalysisReqVO lastPrevReq = new BusinessAnalysisReqVO();
                lastPrevReq.setYear(year - 1);
                lastPrevReq.setCutoffDate(lastPrevEnd);
                lastPrevReq.setLevel(1);
                lastPrevReq.setIncludeEmployees(false);
                BusinessAnalysisRespVO lastPrevData = businessAnalysisService.getBusinessAnalysis(lastPrevReq, userId);
                if (lastPrevData != null && lastPrevData.getTotal() != null) {
                    lastPrevProfit = orZero(lastPrevData.getTotal().getNetProfit());
                }
            }
            BigDecimal lastMonthProfit = lastCumProfit.subtract(lastPrevProfit);
            if (lastMonthProfit.compareTo(BigDecimal.ZERO) > 0) {
                return currentProfit.subtract(lastMonthProfit)
                        .multiply(new BigDecimal("100"))
                        .divide(lastMonthProfit, 1, java.math.RoundingMode.HALF_UP);
            } else if (currentProfit.compareTo(BigDecimal.ZERO) > 0) {
                return new BigDecimal("100");
            }
        } catch (Exception e) {
            log.warn("计算同比增长率失败", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算最近7天收入增量
     */
    private BigDecimal computeWeeklyRevenue(Long userId) {
        try {
            LocalDate now = LocalDate.now();
            int year = now.getYear();

            BusinessAnalysisReqVO curReq = new BusinessAnalysisReqVO();
            curReq.setYear(year);
            curReq.setCutoffDate(now);
            curReq.setLevel(1);
            curReq.setIncludeEmployees(false);
            BusinessAnalysisRespVO curData = businessAnalysisService.getBusinessAnalysis(curReq, userId);
            BigDecimal curRevenue = BigDecimal.ZERO;
            if (curData != null && curData.getTotal() != null) {
                curRevenue = orZero(curData.getTotal().getTotalIncome());
            }

            LocalDate weekAgo = now.minusDays(7);
            int weekAgoYear = weekAgo.getYear();
            BusinessAnalysisReqVO prevReq = new BusinessAnalysisReqVO();
            prevReq.setYear(weekAgoYear);
            prevReq.setCutoffDate(weekAgo);
            prevReq.setLevel(1);
            prevReq.setIncludeEmployees(false);
            BusinessAnalysisRespVO prevData = businessAnalysisService.getBusinessAnalysis(prevReq, userId);
            BigDecimal prevRevenue = BigDecimal.ZERO;
            if (prevData != null && prevData.getTotal() != null) {
                prevRevenue = orZero(prevData.getTotal().getTotalIncome());
            }

            if (weekAgoYear != year) {
                return curRevenue;
            }
            return curRevenue.subtract(prevRevenue);
        } catch (Exception e) {
            log.warn("计算本周收入失败", e);
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    public ReceivableStats getReceivableStats(Long userId) {
        if (dashboardCrmApi != null) {
            ReceivableStats real = dashboardCrmApi.getReceivableStats(userId);
            if (real != null) {
                return real;
            }
        }
        // 降级：返回模拟数据
        boolean admin = isAdmin(userId);
        if (admin) {
            return ReceivableStats.builder()
                    .pendingCount(15)
                    .pendingAmount(new BigDecimal("2350000"))
                    .overdueCount(3)
                    .monthlyReceivedAmount(new BigDecimal("580000"))
                    .build();
        } else {
            return ReceivableStats.builder()
                    .pendingCount(5)
                    .pendingAmount(new BigDecimal("350000"))
                    .overdueCount(1)
                    .monthlyReceivedAmount(new BigDecimal("120000"))
                    .build();
        }
    }

    @Override
    public List<DashboardStatisticsRespVO.MonthlySnapshot> getMonthlySnapshots(Long userId, Integer year) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null) ? year : now.getYear();
        int maxMonth = (targetYear == now.getYear()) ? now.getMonthValue() : 12;

        List<DashboardStatisticsRespVO.MonthlySnapshot> snapshots = new ArrayList<>();

        if (businessAnalysisService == null) {
            return snapshots;
        }

        try {
            // 并行获取0~maxMonth共(maxMonth+1)个累计截面
            @SuppressWarnings("unchecked")
            CompletableFuture<BusinessAnalysisRespVO>[] futures = new CompletableFuture[maxMonth + 1];

            for (int m = 0; m <= maxMonth; m++) {
                LocalDate cutoff;
                int reqYear;
                if (m == 0) {
                    // 上一年年底
                    cutoff = LocalDate.of(targetYear - 1, 12, 31);
                    reqYear = targetYear - 1;
                } else if (m == maxMonth && targetYear == now.getYear()) {
                    cutoff = now;
                    reqYear = targetYear;
                } else {
                    cutoff = LocalDate.of(targetYear, m, 1).plusMonths(1).minusDays(1);
                    reqYear = targetYear;
                }
                final int fy = reqYear;
                final LocalDate fc = cutoff;
                futures[m] = runAsync(() -> fetchBusinessAnalysis(fy, fc, userId));
            }

            CompletableFuture.allOf(futures).join();

            BigDecimal[] cumRevenue = new BigDecimal[maxMonth + 1];
            BigDecimal[] cumCost = new BigDecimal[maxMonth + 1];
            BigDecimal[] cumProfit = new BigDecimal[maxMonth + 1];
            BusinessAnalysisRespVO[] rawData = new BusinessAnalysisRespVO[maxMonth + 1];

            for (int i = 0; i <= maxMonth; i++) {
                rawData[i] = futures[i].join();
                if (rawData[i] != null && rawData[i].getTotal() != null) {
                    cumRevenue[i] = orZero(rawData[i].getTotal().getTotalIncome());
                    cumCost[i] = orZero(rawData[i].getTotal().getTotalExpense());
                    cumProfit[i] = orZero(rawData[i].getTotal().getNetProfit());
                } else {
                    cumRevenue[i] = BigDecimal.ZERO;
                    cumCost[i] = BigDecimal.ZERO;
                    cumProfit[i] = BigDecimal.ZERO;
                }
            }

            BigDecimal weeklyRevenue = computeWeeklyRevenue(userId);
            BigDecimal contractYearlyRev = getContractYearlyTotal(targetYear);

            // 逐月快照
            for (int m = 1; m <= maxMonth; m++) {
                BigDecimal mRevenue = cumRevenue[m].subtract(m == 1 ? BigDecimal.ZERO : cumRevenue[m - 1]);
                BigDecimal mCost = cumCost[m].subtract(m == 1 ? BigDecimal.ZERO : cumCost[m - 1]);
                BigDecimal mProfit = cumProfit[m].subtract(m == 1 ? BigDecimal.ZERO : cumProfit[m - 1]);

                RevenueStats rev = RevenueStats.builder()
                        .monthlyRevenue(mRevenue).monthlyCost(mCost).monthlyProfit(mProfit)
                        .yearlyRevenue(cumRevenue[m]).weeklyRevenue(weeklyRevenue)
                        .contractYearlyRevenue(contractYearlyRev)
                        .growthRate(BigDecimal.ZERO).build();

                List<RankData> rank = buildMonthDeptRank(rawData[m], m > 1 ? rawData[m - 1] : null);

                snapshots.add(DashboardStatisticsRespVO.MonthlySnapshot.builder()
                        .month(m).revenue(rev).deptRanking(rank).build());
            }

            // 总表快照（YTD）
            int monthsPassed = Math.max(maxMonth, 1);
            BigDecimal avgRevenue = cumRevenue[maxMonth].divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal avgCost = cumCost[maxMonth].divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal avgProfit = cumProfit[maxMonth].divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);

            RevenueStats totalRev = RevenueStats.builder()
                    .monthlyRevenue(avgRevenue).monthlyCost(avgCost).monthlyProfit(avgProfit)
                    .yearlyRevenue(cumRevenue[maxMonth]).weeklyRevenue(weeklyRevenue)
                    .contractYearlyRevenue(contractYearlyRev)
                    .growthRate(BigDecimal.ZERO).build();

            List<RankData> totalRank = buildRankDataFromAnalysis(rawData[maxMonth]);

            snapshots.add(DashboardStatisticsRespVO.MonthlySnapshot.builder()
                    .month(null).revenue(totalRev).deptRanking(totalRank != null ? totalRank : new ArrayList<>()).build());

        } catch (Exception e) {
            log.warn("批量获取月度快照失败", e);
        }

        return snapshots;
    }

    private List<RankData> buildMonthDeptRank(BusinessAnalysisRespVO curData, BusinessAnalysisRespVO prevData) {
        if (curData == null || curData.getDeptAnalysisList() == null) {
            return new ArrayList<>();
        }
        Map<String, BigDecimal> prevProfitMap = new java.util.HashMap<>();
        if (prevData != null && prevData.getDeptAnalysisList() != null) {
            for (BusinessAnalysisRespVO.DeptAnalysis dept : prevData.getDeptAnalysisList()) {
                prevProfitMap.put(dept.getDeptName(), orZero(dept.getNetProfit()));
            }
        }
        List<RankData> list = new ArrayList<>();
        for (BusinessAnalysisRespVO.DeptAnalysis dept : curData.getDeptAnalysisList()) {
            BigDecimal curProfit = orZero(dept.getNetProfit());
            BigDecimal prevProfit = prevProfitMap.getOrDefault(dept.getDeptName(), BigDecimal.ZERO);
            list.add(RankData.builder()
                    .deptName(dept.getDeptName())
                    .amount(curProfit.subtract(prevProfit))
                    .completionRate(orZero(dept.getProfitRate()))
                    .build());
        }
        list.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }
        return list;
    }

    /**
     * 获取经营分析数据（优先使用缓存服务）
     */
    private BusinessAnalysisRespVO fetchBusinessAnalysis(int year, LocalDate cutoffDate, Long userId) {
        if (businessAnalysisCacheService != null) {
            return businessAnalysisCacheService.getBusinessAnalysis(year, cutoffDate, userId);
        }
        BusinessAnalysisReqVO reqVO = new BusinessAnalysisReqVO();
        reqVO.setYear(year);
        reqVO.setCutoffDate(cutoffDate);
        reqVO.setLevel(1);
        reqVO.setIncludeEmployees(false);
        return businessAnalysisService.getBusinessAnalysis(reqVO, userId);
    }

    /**
     * 获取经营趋势数据（最近12个月）
     * 优先从经营分析服务获取，保持与部门利润排行数据源一致
     * 
     * 【性能优化】使用 CompletableFuture 并行查询 13 个月的数据 + Redis 缓存
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<TrendData> getTrendData(Long userId, boolean isAdmin) {
        List<TrendData> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月");
        
        if (businessAnalysisCacheService != null || businessAnalysisService != null) {
            try {
                BigDecimal[] cumulativeRevenues = new BigDecimal[13];
                BigDecimal[] cumulativeCosts = new BigDecimal[13];
                BigDecimal[] cumulativeProfits = new BigDecimal[13];
                
                // 【性能优化】并行查询 13 个月的经营分析数据（利用 Redis 缓存 + CompletableFuture）
                @SuppressWarnings("unchecked")
                CompletableFuture<BusinessAnalysisRespVO>[] futures = new CompletableFuture[13];
                
                LocalDate baseMonth = now.minusMonths(12);
                LocalDate baseMonthEnd = baseMonth.withDayOfMonth(baseMonth.lengthOfMonth());
                futures[0] = runAsync(() ->
                        fetchBusinessAnalysis(baseMonthEnd.getYear(), baseMonthEnd, userId));
                
                for (int i = 11; i >= 0; i--) {
                    LocalDate month = now.minusMonths(i);
                    LocalDate monthEnd = (i == 0) ? now : month.withDayOfMonth(month.lengthOfMonth());
                    int idx = 12 - i;
                    final int year = monthEnd.getYear();
                    final LocalDate cutoff = monthEnd;
                    futures[idx] = runAsync(() ->
                            fetchBusinessAnalysis(year, cutoff, userId));
                }
                
                CompletableFuture.allOf(futures).join();
                
                for (int idx = 0; idx < 13; idx++) {
                    BusinessAnalysisRespVO data = futures[idx].join();
                    if (data != null && data.getTotal() != null) {
                        cumulativeRevenues[idx] = data.getTotal().getTotalIncome() != null ? data.getTotal().getTotalIncome() : BigDecimal.ZERO;
                        cumulativeCosts[idx] = data.getTotal().getTotalExpense() != null ? data.getTotal().getTotalExpense() : BigDecimal.ZERO;
                        cumulativeProfits[idx] = data.getTotal().getNetProfit() != null ? data.getTotal().getNetProfit() : BigDecimal.ZERO;
                    } else if (idx > 0) {
                        cumulativeRevenues[idx] = cumulativeRevenues[idx - 1];
                        cumulativeCosts[idx] = cumulativeCosts[idx - 1];
                        cumulativeProfits[idx] = cumulativeProfits[idx - 1];
                    } else {
                        cumulativeRevenues[idx] = BigDecimal.ZERO;
                        cumulativeCosts[idx] = BigDecimal.ZERO;
                        cumulativeProfits[idx] = BigDecimal.ZERO;
                    }
                }
                
                // 计算每月增量（本月累计 - 上月累计）
                boolean hasValidData = false;
                for (int i = 11; i >= 0; i--) {
                    LocalDate month = now.minusMonths(i);
                    int idx = 12 - i;
                    
                    // 跨年时需要特殊处理：不同年份的累计数据不能直接相减
                    // 简化处理：如果是1月份，本月增量就是本月累计（因为是新年第一个月）
                    BigDecimal monthRevenue, monthCost, monthProfit;
                    if (month.getMonthValue() == 1) {
                        // 1月份：本年累计就是1月数据
                        monthRevenue = cumulativeRevenues[idx];
                        monthCost = cumulativeCosts[idx];
                        monthProfit = cumulativeProfits[idx];
                    } else {
                        // 非1月份：本月 = 本月累计 - 上月累计
                        monthRevenue = cumulativeRevenues[idx].subtract(cumulativeRevenues[idx - 1]);
                        monthCost = cumulativeCosts[idx].subtract(cumulativeCosts[idx - 1]);
                        monthProfit = cumulativeProfits[idx].subtract(cumulativeProfits[idx - 1]);
                    }
                    
                    // 转换为万元显示（与图表单位一致）
                    BigDecimal revenueWan = monthRevenue.divide(new BigDecimal("10000"), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal costWan = monthCost.divide(new BigDecimal("10000"), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal profitWan = monthProfit.divide(new BigDecimal("10000"), 2, java.math.RoundingMode.HALF_UP);
                    
                    if (revenueWan.compareTo(BigDecimal.ZERO) != 0 || costWan.compareTo(BigDecimal.ZERO) != 0) {
                        hasValidData = true;
                    }
                    
                    list.add(TrendData.builder()
                            .month(month.format(formatter))
                            .revenue(revenueWan)
                            .cost(costWan)
                            .profit(profitWan)
                            .build());
                }
                
                if (hasValidData) {
                    return list;
                }
            } catch (Exception e) {
                log.warn("从经营分析服务获取趋势数据失败，使用降级方案", e);
                list.clear();
            }
        }
        
        // 降级：尝试从 CRM 获取
        if (dashboardCrmApi != null) {
            List<TrendData> real = dashboardCrmApi.getTrendData(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                boolean hasValidData = real.stream().anyMatch(t -> 
                    (t.getRevenue() != null && t.getRevenue().compareTo(BigDecimal.ZERO) > 0) ||
                    (t.getProfit() != null && t.getProfit().compareTo(BigDecimal.ZERO) != 0));
                if (hasValidData) {
                    return real;
                }
            }
        }
        
        // 最后降级：使用模拟数据
        list.clear();
        int[] revenues;
        int[] costs;
        
        if (isAdmin) {
            revenues = new int[]{120, 132, 101, 134, 90, 230, 210, 182, 191, 234, 290, 330};
            costs = new int[]{80, 92, 71, 94, 60, 150, 130, 112, 121, 154, 180, 200};
        } else {
            revenues = new int[]{15, 18, 12, 20, 8, 25, 22, 19, 21, 28, 32, 35};
            costs = new int[]{8, 10, 7, 12, 5, 14, 12, 10, 11, 15, 18, 20};
        }

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            int idx = 11 - i;
            BigDecimal revenue = BigDecimal.valueOf(revenues[idx]);
            BigDecimal cost = BigDecimal.valueOf(costs[idx]);
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
        // 优先从项目模块获取真实数据
        if (dashboardProjectApi != null) {
            List<PieChartData> real = dashboardProjectApi.getProjectDistribution(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }
        
        // 降级：使用模拟数据
        List<PieChartData> list = new ArrayList<>();
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

    @Override
    public List<RankData> getDeptRanking(Long userId, Integer year, Integer month) {
        boolean isAdmin = isAdmin(userId);
        return getDeptRankingInternal(userId, isAdmin, year, month);
    }

    private List<RankData> getDeptRanking(Long userId, boolean isAdmin) {
        return getDeptRankingInternal(userId, isAdmin, null, null);
    }

    /**
     * 内部方法：获取部门排行，支持按月筛选
     */
    private List<RankData> getDeptRankingInternal(Long userId, boolean isAdmin, Integer year, Integer month) {
        if (month == null && businessAnalysisCacheService != null) {
            try {
                List<RankData> cachedRanking = businessAnalysisCacheService.getDeptRanking(userId);
                if (cachedRanking != null && !cachedRanking.isEmpty()) {
                    return cachedRanking;
                }
            } catch (Exception e) {
                log.warn("从缓存服务获取部门排行失败，尝试直接计算", e);
            }
        }

        if (businessAnalysisService != null) {
            try {
                LocalDate now = LocalDate.now();
                int targetYear = (year != null) ? year : now.getYear();

                if (month != null && month >= 1 && month <= 12) {
                    return getDeptRankingForMonth(userId, targetYear, month);
                }

                LocalDate cutoff = (targetYear == now.getYear()) ? now : LocalDate.of(targetYear, 12, 31);
                BusinessAnalysisReqVO reqVO = new BusinessAnalysisReqVO();
                reqVO.setYear(targetYear);
                reqVO.setCutoffDate(cutoff);
                reqVO.setLevel(1);
                reqVO.setIncludeEmployees(false);
                BusinessAnalysisRespVO analysisData = businessAnalysisService.getBusinessAnalysis(reqVO, userId);

                List<RankData> result = buildRankDataFromAnalysis(analysisData);
                if (result != null) return result;
            } catch (Exception e) {
                log.warn("从经营分析服务获取部门排行数据失败，使用降级方案", e);
            }
        }

        if (dashboardCrmApi != null) {
            List<RankData> real = dashboardCrmApi.getDeptRanking(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }

        return new ArrayList<>();
    }

    private List<RankData> getDeptRankingForMonth(Long userId, int year, int month) {
        LocalDate monthEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        LocalDate cutoff = monthEnd.isAfter(LocalDate.now()) ? LocalDate.now() : monthEnd;

        BusinessAnalysisReqVO curReq = new BusinessAnalysisReqVO();
        curReq.setYear(year);
        curReq.setCutoffDate(cutoff);
        curReq.setLevel(1);
        curReq.setIncludeEmployees(false);
        BusinessAnalysisRespVO curData = businessAnalysisService.getBusinessAnalysis(curReq, userId);

        BusinessAnalysisRespVO prevData = null;
        if (month > 1) {
            LocalDate prevEnd = LocalDate.of(year, month - 1, 1).plusMonths(1).minusDays(1);
            BusinessAnalysisReqVO prevReq = new BusinessAnalysisReqVO();
            prevReq.setYear(year);
            prevReq.setCutoffDate(prevEnd);
            prevReq.setLevel(1);
            prevReq.setIncludeEmployees(false);
            prevData = businessAnalysisService.getBusinessAnalysis(prevReq, userId);
        }

        if (curData == null || curData.getDeptAnalysisList() == null) {
            return new ArrayList<>();
        }

        Map<String, BigDecimal> prevProfitMap = new java.util.HashMap<>();
        if (prevData != null && prevData.getDeptAnalysisList() != null) {
            for (BusinessAnalysisRespVO.DeptAnalysis dept : prevData.getDeptAnalysisList()) {
                prevProfitMap.put(dept.getDeptName(), orZero(dept.getNetProfit()));
            }
        }

        List<RankData> list = new ArrayList<>();
        for (BusinessAnalysisRespVO.DeptAnalysis dept : curData.getDeptAnalysisList()) {
            BigDecimal curProfit = orZero(dept.getNetProfit());
            BigDecimal prevProfit = prevProfitMap.getOrDefault(dept.getDeptName(), BigDecimal.ZERO);
            BigDecimal monthProfit = curProfit.subtract(prevProfit);
            BigDecimal profitRate = orZero(dept.getProfitRate());
            list.add(RankData.builder()
                    .deptName(dept.getDeptName())
                    .amount(monthProfit)
                    .completionRate(profitRate)
                    .build());
        }
        list.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }
        return list;
    }

    private List<RankData> buildRankDataFromAnalysis(BusinessAnalysisRespVO analysisData) {
        if (analysisData == null || analysisData.getDeptAnalysisList() == null || analysisData.getDeptAnalysisList().isEmpty()) {
            return null;
        }
        List<RankData> list = new ArrayList<>();
        List<BusinessAnalysisRespVO.DeptAnalysis> deptList = new ArrayList<>(analysisData.getDeptAnalysisList());
        deptList.sort((a, b) -> orZero(b.getNetProfit()).compareTo(orZero(a.getNetProfit())));
        for (int i = 0; i < deptList.size(); i++) {
            BusinessAnalysisRespVO.DeptAnalysis dept = deptList.get(i);
            list.add(RankData.builder()
                    .rank(i + 1)
                    .deptName(dept.getDeptName())
                    .amount(orZero(dept.getNetProfit()))
                    .completionRate(orZero(dept.getProfitRate()))
                    .build());
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
