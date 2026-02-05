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
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.dal.dataobject.logger.OperateLogDO;
import cn.shuhe.system.module.system.service.cost.BusinessAnalysisCacheService;
import cn.shuhe.system.module.system.service.cost.BusinessAnalysisService;
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

    @Autowired(required = false)
    private BusinessAnalysisService businessAnalysisService;

    @Autowired(required = false)
    private BusinessAnalysisCacheService businessAnalysisCacheService;

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
        // 【性能优化】优先从缓存服务获取收入统计
        if (businessAnalysisCacheService != null) {
            try {
                RevenueStats cachedStats = businessAnalysisCacheService.getRevenueStats(userId);
                if (cachedStats != null) {
                    return cachedStats;
                }
            } catch (Exception e) {
                log.warn("从缓存服务获取收入统计失败，尝试直接计算", e);
            }
        }
        
        // 降级：直接从经营分析服务获取（无缓存）
        if (businessAnalysisService != null) {
            try {
                LocalDate now = LocalDate.now();
                int currentYear = now.getYear();
                
                // 获取年度累计数据（截至当前）
                BusinessAnalysisReqVO yearReqVO = new BusinessAnalysisReqVO();
                yearReqVO.setYear(currentYear);
                yearReqVO.setCutoffDate(now);
                yearReqVO.setLevel(1); // 部门汇总级别
                yearReqVO.setIncludeEmployees(false); // 不包含员工详情，加快查询
                BusinessAnalysisRespVO yearData = businessAnalysisService.getBusinessAnalysis(yearReqVO, userId);
                
                if (yearData != null && yearData.getTotal() != null) {
                    BusinessAnalysisRespVO.TotalAnalysis total = yearData.getTotal();
                    BigDecimal yearlyRevenue = total.getTotalIncome() != null ? total.getTotalIncome() : BigDecimal.ZERO;
                    BigDecimal yearlyCost = total.getTotalExpense() != null ? total.getTotalExpense() : BigDecimal.ZERO;
                    BigDecimal yearlyProfit = total.getNetProfit() != null ? total.getNetProfit() : BigDecimal.ZERO;
                    
                    // 简化：本月数据直接使用年度累计数据除以已过月份
                    int monthsPassed = Math.max(now.getMonthValue(), 1);
                    BigDecimal monthlyRevenue = yearlyRevenue.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal monthlyCost = yearlyCost.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal monthlyProfit = yearlyProfit.divide(new BigDecimal(monthsPassed), 2, java.math.RoundingMode.HALF_UP);
                    
                    // 计算同比增长率（与去年同期比较）
                    BigDecimal growthRate = BigDecimal.ZERO;
                    try {
                        BusinessAnalysisReqVO lastYearReqVO = new BusinessAnalysisReqVO();
                        lastYearReqVO.setYear(currentYear - 1);
                        lastYearReqVO.setCutoffDate(now.minusYears(1));
                        lastYearReqVO.setLevel(1);
                        lastYearReqVO.setIncludeEmployees(false);
                        BusinessAnalysisRespVO lastYearData = businessAnalysisService.getBusinessAnalysis(lastYearReqVO, userId);
                        
                        if (lastYearData != null && lastYearData.getTotal() != null) {
                            BigDecimal lastYearProfit = lastYearData.getTotal().getNetProfit();
                            if (lastYearProfit != null && lastYearProfit.compareTo(BigDecimal.ZERO) > 0) {
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
                    
                    return RevenueStats.builder()
                            .monthlyRevenue(monthlyRevenue)
                            .monthlyCost(monthlyCost)
                            .monthlyProfit(monthlyProfit)
                            .growthRate(growthRate)
                            .yearlyRevenue(yearlyRevenue)
                            .build();
                }
            } catch (Exception e) {
                log.warn("从经营分析服务获取利润数据失败，使用降级方案", e);
            }
        }
        
        // 降级：尝试从 CRM 回款获取数据
        if (dashboardCrmApi != null) {
            RevenueStats real = dashboardCrmApi.getRevenueStats(userId);
            if (real != null) {
                return real;
            }
        }
        
        // 最后降级：返回模拟数据
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

    /**
     * 获取经营趋势数据（最近12个月）
     * 优先从经营分析服务获取，保持与部门利润排行数据源一致
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<TrendData> getTrendData(Long userId, boolean isAdmin) {
        List<TrendData> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月");
        
        // 优先从经营分析服务获取真实数据（与部门利润排行数据源一致）
        if (businessAnalysisService != null) {
            try {
                // 存储每个月末的累计数据，用于计算月度增量
                BigDecimal[] cumulativeRevenues = new BigDecimal[13]; // 0为上月末基准
                BigDecimal[] cumulativeCosts = new BigDecimal[13];
                BigDecimal[] cumulativeProfits = new BigDecimal[13];
                
                // 获取12个月前的月末累计作为基准
                LocalDate baseMonth = now.minusMonths(12);
                LocalDate baseMonthEnd = baseMonth.withDayOfMonth(baseMonth.lengthOfMonth());
                BusinessAnalysisReqVO baseReq = new BusinessAnalysisReqVO();
                baseReq.setYear(baseMonthEnd.getYear());
                baseReq.setCutoffDate(baseMonthEnd);
                baseReq.setLevel(1);
                baseReq.setIncludeEmployees(false);
                
                BusinessAnalysisRespVO baseData = businessAnalysisService.getBusinessAnalysis(baseReq, userId);
                if (baseData != null && baseData.getTotal() != null) {
                    cumulativeRevenues[0] = baseData.getTotal().getTotalIncome() != null ? baseData.getTotal().getTotalIncome() : BigDecimal.ZERO;
                    cumulativeCosts[0] = baseData.getTotal().getTotalExpense() != null ? baseData.getTotal().getTotalExpense() : BigDecimal.ZERO;
                    cumulativeProfits[0] = baseData.getTotal().getNetProfit() != null ? baseData.getTotal().getNetProfit() : BigDecimal.ZERO;
                } else {
                    cumulativeRevenues[0] = BigDecimal.ZERO;
                    cumulativeCosts[0] = BigDecimal.ZERO;
                    cumulativeProfits[0] = BigDecimal.ZERO;
                }
                
                // 获取最近12个月每个月末的累计数据
                for (int i = 11; i >= 0; i--) {
                    LocalDate month = now.minusMonths(i);
                    LocalDate monthEnd = (i == 0) ? now : month.withDayOfMonth(month.lengthOfMonth());
                    int idx = 12 - i; // 1~12
                    
                    BusinessAnalysisReqVO reqVO = new BusinessAnalysisReqVO();
                    reqVO.setYear(monthEnd.getYear());
                    reqVO.setCutoffDate(monthEnd);
                    reqVO.setLevel(1);
                    reqVO.setIncludeEmployees(false);
                    
                    BusinessAnalysisRespVO data = businessAnalysisService.getBusinessAnalysis(reqVO, userId);
                    if (data != null && data.getTotal() != null) {
                        cumulativeRevenues[idx] = data.getTotal().getTotalIncome() != null ? data.getTotal().getTotalIncome() : BigDecimal.ZERO;
                        cumulativeCosts[idx] = data.getTotal().getTotalExpense() != null ? data.getTotal().getTotalExpense() : BigDecimal.ZERO;
                        cumulativeProfits[idx] = data.getTotal().getNetProfit() != null ? data.getTotal().getNetProfit() : BigDecimal.ZERO;
                    } else {
                        cumulativeRevenues[idx] = cumulativeRevenues[idx - 1];
                        cumulativeCosts[idx] = cumulativeCosts[idx - 1];
                        cumulativeProfits[idx] = cumulativeProfits[idx - 1];
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

    /**
     * 获取部门排行（只有管理员可见）
     * 从经营分析服务获取各部门的利润数据
     * 【性能优化】优先从缓存服务获取
     * 
     * @param userId 用户ID
     * @param isAdmin 是否管理员
     */
    private List<RankData> getDeptRanking(Long userId, boolean isAdmin) {
        if (!isAdmin) {
            return List.of();
        }
        
        // 【性能优化】优先从缓存服务获取部门排行
        if (businessAnalysisCacheService != null) {
            try {
                List<RankData> cachedRanking = businessAnalysisCacheService.getDeptRanking(userId);
                if (cachedRanking != null && !cachedRanking.isEmpty()) {
                    return cachedRanking;
                }
            } catch (Exception e) {
                log.warn("从缓存服务获取部门排行失败，尝试直接计算", e);
            }
        }
        
        // 降级：直接从经营分析服务获取（无缓存）
        if (businessAnalysisService != null) {
            try {
                LocalDate now = LocalDate.now();
                BusinessAnalysisReqVO reqVO = new BusinessAnalysisReqVO();
                reqVO.setYear(now.getYear());
                reqVO.setCutoffDate(now);
                reqVO.setLevel(1); // 部门汇总级别
                reqVO.setIncludeEmployees(false); // 不包含员工详情，加快查询
                BusinessAnalysisRespVO analysisData = businessAnalysisService.getBusinessAnalysis(reqVO, userId);
                
                if (analysisData != null && analysisData.getDeptAnalysisList() != null && !analysisData.getDeptAnalysisList().isEmpty()) {
                    List<RankData> list = new ArrayList<>();
                    List<BusinessAnalysisRespVO.DeptAnalysis> deptList = new ArrayList<>(analysisData.getDeptAnalysisList());
                    
                    // 按利润排序
                    deptList.sort((a, b) -> {
                        BigDecimal profitA = a.getNetProfit() != null ? a.getNetProfit() : BigDecimal.ZERO;
                        BigDecimal profitB = b.getNetProfit() != null ? b.getNetProfit() : BigDecimal.ZERO;
                        return profitB.compareTo(profitA);
                    });
                    
                    for (int i = 0; i < deptList.size(); i++) {
                        BusinessAnalysisRespVO.DeptAnalysis dept = deptList.get(i);
                        BigDecimal profit = dept.getNetProfit() != null ? dept.getNetProfit() : BigDecimal.ZERO;
                        BigDecimal profitRate = dept.getProfitRate() != null ? dept.getProfitRate() : BigDecimal.ZERO;
                        
                        list.add(RankData.builder()
                                .rank(i + 1)
                                .deptName(dept.getDeptName())
                                .amount(profit)
                                .completionRate(profitRate)
                                .build());
                    }
                    return list;
                }
            } catch (Exception e) {
                log.warn("从经营分析服务获取部门排行数据失败，使用降级方案", e);
            }
        }
        
        // 降级：尝试从 CRM 获取
        if (dashboardCrmApi != null) {
            List<RankData> real = dashboardCrmApi.getDeptRanking(userId, isAdmin);
            if (real != null && !real.isEmpty()) {
                return real;
            }
        }
        
        // 最后降级：返回模拟数据
        List<RankData> list = new ArrayList<>();
        list.add(RankData.builder().rank(1).deptName("渗透测试部").amount(new BigDecimal("3250000")).completionRate(new BigDecimal("125.5")).build());
        list.add(RankData.builder().rank(2).deptName("安全运营部").amount(new BigDecimal("2860000")).completionRate(new BigDecimal("110.2")).build());
        list.add(RankData.builder().rank(3).deptName("安全服务部").amount(new BigDecimal("2150000")).completionRate(new BigDecimal("95.8")).build());
        list.add(RankData.builder().rank(4).deptName("安全集成部").amount(new BigDecimal("1680000")).completionRate(new BigDecimal("88.4")).build());
        list.add(RankData.builder().rank(5).deptName("研发部").amount(new BigDecimal("920000")).completionRate(new BigDecimal("76.5")).build());
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
