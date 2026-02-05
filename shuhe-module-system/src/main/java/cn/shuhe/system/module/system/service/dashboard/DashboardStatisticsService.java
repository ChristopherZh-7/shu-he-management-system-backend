package cn.shuhe.system.module.system.service.dashboard;

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
     * 获取收入统计
     *
     * @param userId 当前用户ID
     */
    DashboardStatisticsRespVO.RevenueStats getRevenueStats(Long userId);

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
