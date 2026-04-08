package cn.shuhe.system.module.finance.service.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;

/**
 * 仪表板统计 Service 接口
 */
public interface DashboardStatisticsService {

    /**
     * 获取仪表板统计数据
     *
     * @param userId   当前用户ID
     * @param pageType 页面类型：analytics 或 workspace
     * @return 统计数据
     */
    DashboardStatisticsRespVO getStatistics(Long userId, String pageType);

    /**
     * 获取项目统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.ProjectStats getProjectStats(Long userId);

    /**
     * 获取合同统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.ContractStats getContractStats(Long userId);

    /**
     * 获取客户统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.CustomerStats getCustomerStats(Long userId);

    /**
     * 获取任务统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.TaskStats getTaskStats(Long userId);

    /**
     * 获取收入统计（当前月/年度汇总）
     */
    DashboardStatisticsRespVO.RevenueStats getRevenueStats(Long userId);

    /**
     * 获取收入统计（指定年月）
     *
     * @param userId 当前用户ID
     * @param year   年份，null 则取当前年
     * @param month  月份(1-12)，null 则返回年度汇总
     */
    DashboardStatisticsRespVO.RevenueStats getRevenueStats(Long userId, Integer year, Integer month);

    /**
     * 获取部门利润排行（指定年月）
     *
     * @param userId 当前用户ID
     * @param year   年份，null 则取当前年
     * @param month  月份(1-12)，null 则返回年度汇总
     */
    java.util.List<DashboardStatisticsRespVO.RankData> getDeptRanking(Long userId, Integer year, Integer month);

    /**
     * 批量获取所有月份的快照数据（收入+部门排行），一次计算减少重复查询
     *
     * @param userId 当前用户ID
     * @param year   年份，null 则取当前年
     */
    java.util.List<DashboardStatisticsRespVO.MonthlySnapshot> getMonthlySnapshots(Long userId, Integer year);

    /**
     * 获取待回款统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.ReceivableStats getReceivableStats(Long userId);

    /**
     * 判断用户是否是管理员（拥有全局数据查看权限）
     *
     * @param userId 用户ID
     * @return 是否是管理员
     */
    boolean isAdmin(Long userId);

}
