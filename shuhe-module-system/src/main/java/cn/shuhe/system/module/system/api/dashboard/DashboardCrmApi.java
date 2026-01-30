package cn.shuhe.system.module.system.api.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;

import java.util.List;

/**
 * 仪表板 - CRM 合同/客户/收入统计 API 接口
 * 由 crm 模块实现，为工作台/分析页提供真实合同、客户、收入数据
 */
public interface DashboardCrmApi {

    /**
     * 获取合同统计
     *
     * @param userId 用户ID
     * @return 合同统计，若未实现可返回 null
     */
    DashboardStatisticsRespVO.ContractStats getContractStats(Long userId);

    /**
     * 获取客户统计
     *
     * @param userId 用户ID
     * @return 客户统计，若未实现可返回 null
     */
    DashboardStatisticsRespVO.CustomerStats getCustomerStats(Long userId);

    /**
     * 获取收入统计
     *
     * @param userId 用户ID
     * @return 收入统计，若未实现可返回 null
     */
    DashboardStatisticsRespVO.RevenueStats getRevenueStats(Long userId);

    /**
     * 获取经营趋势（最近12个月）
     *
     * @param userId  用户ID
     * @param isAdmin 是否管理员
     * @return 趋势数据，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.TrendData> getTrendData(Long userId, boolean isAdmin);

    /**
     * 获取即将到期合同提醒
     *
     * @param userId  用户ID
     * @param isAdmin 是否管理员
     * @return 合同提醒列表，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.ContractRemindItem> getContractReminders(Long userId, boolean isAdmin);

    /**
     * 获取部门利润/业绩排行（按部门汇总回款金额，仅管理员看全部部门）
     *
     * @param userId  用户ID
     * @param isAdmin 是否管理员
     * @return 部门排行，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.RankData> getDeptRanking(Long userId, boolean isAdmin);
}
