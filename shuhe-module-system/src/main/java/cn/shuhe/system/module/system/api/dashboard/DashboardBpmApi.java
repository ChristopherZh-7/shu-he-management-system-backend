package cn.shuhe.system.module.system.api.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;

import java.util.List;

/**
 * 仪表板 - BPM 待办/任务统计 API 接口
 * 由 bpm 模块实现，为工作台/分析页提供真实待办与任务数据
 */
public interface DashboardBpmApi {

    /**
     * 获取任务统计（待办数、今日完成、本周完成、逾期数）
     *
     * @param userId 用户ID
     * @return 任务统计，若未实现可返回 null
     */
    DashboardStatisticsRespVO.TaskStats getTaskStats(Long userId);

    /**
     * 获取当前用户待办列表（用于工作台「我的待办」）
     *
     * @param userId 用户ID
     * @param limit  条数上限
     * @return 待办项列表，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.TodoItem> getTodoList(Long userId, int limit);

    /**
     * 获取任务状态分布（饼图）
     *
     * @param userId  用户ID
     * @param isAdmin 是否管理员
     * @return 任务分布数据，若未实现可返回 null
     */
    List<DashboardStatisticsRespVO.PieChartData> getTaskDistribution(Long userId, boolean isAdmin);
}
