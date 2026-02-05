package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO.RankData;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO.RevenueStats;

import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析缓存服务
 * 提供缓存管理功能，加速经营分析和工作台数据查询
 *
 * @author system
 */
public interface BusinessAnalysisCacheService {

    /**
     * 获取经营分析数据（带缓存）
     *
     * @param year       年份
     * @param cutoffDate 截止日期
     * @param userId     当前用户ID（用于权限判断）
     * @return 经营分析结果
     */
    BusinessAnalysisRespVO getBusinessAnalysis(int year, LocalDate cutoffDate, Long userId);

    /**
     * 获取收入统计（带缓存）
     *
     * @param userId 用户ID
     * @return 收入统计
     */
    RevenueStats getRevenueStats(Long userId);

    /**
     * 获取部门排行（带缓存）
     *
     * @param userId 用户ID
     * @return 部门排行列表
     */
    List<RankData> getDeptRanking(Long userId);

    /**
     * 刷新经营分析缓存
     * 在数据变更后调用，主动刷新缓存
     *
     * @param year 年份
     */
    void refreshCache(int year);

    /**
     * 清除所有经营分析相关缓存
     */
    void clearAllCache();

    /**
     * 预计算并缓存今年的经营分析数据
     * 供定时任务调用
     */
    void precomputeCurrentYearData();
}
