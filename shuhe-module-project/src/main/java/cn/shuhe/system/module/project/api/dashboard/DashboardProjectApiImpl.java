package cn.shuhe.system.module.project.api.dashboard;

import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.system.api.dashboard.DashboardProjectApi;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仪表板 - 项目统计 API 实现
 * 为工作台/分析页提供真实项目数据
 */
@Service
public class DashboardProjectApiImpl implements DashboardProjectApi {

    @Resource
    private ProjectService projectService;

    @Override
    public DashboardStatisticsRespVO.ProjectStats getProjectStats(Long userId) {
        return projectService.getProjectStats(userId);
    }

    @Override
    public List<DashboardStatisticsRespVO.PieChartData> getProjectDistribution(Long userId, boolean isAdmin) {
        return projectService.getProjectDistribution(userId, isAdmin);
    }
}
