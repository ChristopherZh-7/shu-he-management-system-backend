package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO.RankData;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO.RevenueStats;
import cn.shuhe.system.module.system.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 经营分析缓存服务实现
 * 使用 Redis 缓存加速经营分析数据查询
 *
 * @author system
 */
@Slf4j
@Service
public class BusinessAnalysisCacheServiceImpl implements BusinessAnalysisCacheService {

    /**
     * 经营分析缓存过期时间（分钟）
     */
    private static final int BUSINESS_ANALYSIS_CACHE_MINUTES = 10;

    /**
     * 收入统计缓存过期时间（分钟）
     */
    private static final int REVENUE_STATS_CACHE_MINUTES = 5;

    /**
     * 部门排行缓存过期时间（分钟）
     */
    private static final int DEPT_RANKING_CACHE_MINUTES = 10;

    @Resource
    private BusinessAnalysisService businessAnalysisService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Cacheable(value = RedisKeyConstants.BUSINESS_ANALYSIS, 
               key = "#year + ':' + #cutoffDate.toString()",
               unless = "#result == null")
    public BusinessAnalysisRespVO getBusinessAnalysis(int year, LocalDate cutoffDate, Long userId) {
        log.info("[缓存未命中] 开始计算经营分析数据，year={}, cutoffDate={}", year, cutoffDate);
        long startTime = System.currentTimeMillis();
        
        BusinessAnalysisReqVO reqVO = new BusinessAnalysisReqVO();
        reqVO.setYear(year);
        reqVO.setCutoffDate(cutoffDate);
        reqVO.setLevel(2); // 包含子部门
        reqVO.setIncludeEmployees(false); // 不包含员工详情，加快查询
        
        // 使用系统管理员ID(1L)获取完整数据，供所有用户共享
        BusinessAnalysisRespVO result = businessAnalysisService.getBusinessAnalysis(reqVO, 1L);
        
        log.info("[缓存未命中] 经营分析计算完成，耗时={}ms", System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Cacheable(value = RedisKeyConstants.DASHBOARD_REVENUE,
               key = "'all'",
               unless = "#result == null")
    public RevenueStats getRevenueStats(Long userId) {
        log.info("[缓存未命中] 开始计算收入统计");
        long startTime = System.currentTimeMillis();
        
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        
        // 获取年度累计数据
        BusinessAnalysisRespVO yearData = getBusinessAnalysis(currentYear, now, userId);
        
        if (yearData == null || yearData.getTotal() == null) {
            log.warn("[收入统计] 无法获取经营分析数据，返回空结果");
            return buildEmptyRevenueStats();
        }
        
        BusinessAnalysisRespVO.TotalAnalysis total = yearData.getTotal();
        BigDecimal yearlyRevenue = total.getTotalIncome() != null ? total.getTotalIncome() : BigDecimal.ZERO;
        BigDecimal yearlyCost = total.getTotalExpense() != null ? total.getTotalExpense() : BigDecimal.ZERO;
        BigDecimal yearlyProfit = total.getNetProfit() != null ? total.getNetProfit() : BigDecimal.ZERO;
        
        // 计算月均
        int monthsPassed = Math.max(now.getMonthValue(), 1);
        BigDecimal monthlyRevenue = yearlyRevenue.divide(new BigDecimal(monthsPassed), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyCost = yearlyCost.divide(new BigDecimal(monthsPassed), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyProfit = yearlyProfit.divide(new BigDecimal(monthsPassed), 2, RoundingMode.HALF_UP);
        
        // 计算同比增长率
        BigDecimal growthRate = calculateGrowthRate(currentYear, now, userId, yearlyProfit);
        
        log.info("[缓存未命中] 收入统计计算完成，耗时={}ms", System.currentTimeMillis() - startTime);
        
        return RevenueStats.builder()
                .monthlyRevenue(monthlyRevenue)
                .monthlyCost(monthlyCost)
                .monthlyProfit(monthlyProfit)
                .growthRate(growthRate)
                .yearlyRevenue(yearlyRevenue)
                .build();
    }

    @Override
    @Cacheable(value = RedisKeyConstants.DASHBOARD_DEPT_RANKING,
               key = "'all'",
               unless = "#result == null || #result.isEmpty()")
    public List<RankData> getDeptRanking(Long userId) {
        log.info("[缓存未命中] 开始计算部门排行");
        long startTime = System.currentTimeMillis();
        
        LocalDate now = LocalDate.now();
        BusinessAnalysisRespVO analysisData = getBusinessAnalysis(now.getYear(), now, userId);
        
        if (analysisData == null || analysisData.getDeptAnalysisList() == null 
                || analysisData.getDeptAnalysisList().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<RankData> result = new ArrayList<>();
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
            
            result.add(RankData.builder()
                    .rank(i + 1)
                    .deptName(dept.getDeptName())
                    .amount(profit)
                    .completionRate(profitRate)
                    .build());
        }
        
        log.info("[缓存未命中] 部门排行计算完成，耗时={}ms", System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @CacheEvict(value = {RedisKeyConstants.BUSINESS_ANALYSIS, RedisKeyConstants.DASHBOARD_REVENUE, 
                         RedisKeyConstants.DASHBOARD_DEPT_RANKING}, 
                allEntries = true)
    public void refreshCache(int year) {
        log.info("[缓存刷新] 清除经营分析相关缓存，year={}", year);
    }

    @Override
    public void clearAllCache() {
        log.info("[缓存清除] 开始清除所有经营分析相关缓存");
        try {
            // 清除经营分析缓存（去除#ttl后缀）
            Set<String> analysisKeys = stringRedisTemplate.keys("business_analysis:*");
            if (analysisKeys != null && !analysisKeys.isEmpty()) {
                stringRedisTemplate.delete(analysisKeys);
                log.info("[缓存清除] 已清除 {} 个经营分析缓存", analysisKeys.size());
            }
            
            // 清除收入统计缓存
            Set<String> revenueKeys = stringRedisTemplate.keys("dashboard_revenue:*");
            if (revenueKeys != null && !revenueKeys.isEmpty()) {
                stringRedisTemplate.delete(revenueKeys);
                log.info("[缓存清除] 已清除 {} 个收入统计缓存", revenueKeys.size());
            }
            
            // 清除部门排行缓存
            Set<String> rankingKeys = stringRedisTemplate.keys("dashboard_dept_ranking:*");
            if (rankingKeys != null && !rankingKeys.isEmpty()) {
                stringRedisTemplate.delete(rankingKeys);
                log.info("[缓存清除] 已清除 {} 个部门排行缓存", rankingKeys.size());
            }
        } catch (Exception e) {
            log.error("[缓存清除] 清除缓存失败", e);
        }
    }

    @Override
    public void precomputeCurrentYearData() {
        log.info("[预计算] 开始预计算当前年度经营分析数据");
        long startTime = System.currentTimeMillis();
        
        try {
            LocalDate now = LocalDate.now();
            int currentYear = now.getYear();
            
            // 先清除缓存
            clearAllCache();
            
            // 使用系统用户ID（1L）预计算数据，这样缓存可以被管理员复用
            Long systemUserId = 1L;
            
            // 预计算经营分析数据
            getBusinessAnalysis(currentYear, now, systemUserId);
            
            // 预计算收入统计
            getRevenueStats(systemUserId);
            
            // 预计算部门排行
            getDeptRanking(systemUserId);
            
            log.info("[预计算] 预计算完成，总耗时={}ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[预计算] 预计算失败", e);
        }
    }

    /**
     * 计算同比增长率
     */
    private BigDecimal calculateGrowthRate(int currentYear, LocalDate now, Long userId, BigDecimal yearlyProfit) {
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
                    return yearlyProfit.subtract(lastYearProfit)
                            .multiply(new BigDecimal("100"))
                            .divide(lastYearProfit, 1, RoundingMode.HALF_UP);
                } else if (yearlyProfit.compareTo(BigDecimal.ZERO) > 0) {
                    return new BigDecimal("100");
                }
            }
        } catch (Exception e) {
            log.warn("[同比计算] 计算同比增长率失败", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 构建空的收入统计
     */
    private RevenueStats buildEmptyRevenueStats() {
        return RevenueStats.builder()
                .monthlyRevenue(BigDecimal.ZERO)
                .monthlyCost(BigDecimal.ZERO)
                .monthlyProfit(BigDecimal.ZERO)
                .growthRate(BigDecimal.ZERO)
                .yearlyRevenue(BigDecimal.ZERO)
                .build();
    }
}
