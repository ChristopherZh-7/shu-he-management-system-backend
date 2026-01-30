package cn.shuhe.system.module.system.api.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;

import java.util.List;

/**
 * 仪表板 - 项目统计 API 接口
 * 由 project 模块实现，为工作台/分析页提供真实项目数据
 */
public interface DashboardProjectApi {

    /**
     * 获取项目统计（进行中、总数、本月新增、已完成）
     *
     * @param userId 用户ID
     * @return 项目统计，若模块未实现可返回 null（仪表板将使用兜底数据）
     */
    DashboardStatisticsRespVO.ProjectStats getProjectStats(Long userId);

    /**
     * 获取项目状态分布（饼图）
     *
     * @param userId  用户ID
     * @param isAdmin 是否管理员
     * @return 项目分布数据，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.PieChartData> getProjectDistribution(Long userId, boolean isAdmin);
}
