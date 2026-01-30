package cn.shuhe.system.module.system.controller.admin.dashboard;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import cn.shuhe.system.module.system.service.dashboard.DashboardStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 仪表板统计 Controller
 * 
 * 权限说明：
 * - 管理员（super_admin）：查看全局统计数据
 * - 普通用户：只能查看自己相关的数据
 */
@Tag(name = "管理后台 - 仪表板统计")
@RestController
@RequestMapping("/system/dashboard-statistics")
@Validated
public class DashboardStatisticsController {

    @Resource
    private DashboardStatisticsService dashboardStatisticsService;

    @GetMapping("/all")
    @Operation(summary = "获取仪表板全部统计数据")
    public CommonResult<DashboardStatisticsRespVO> getAllStatistics(
            @Parameter(description = "页面类型：analytics 或 workspace")
            @RequestParam(defaultValue = "analytics") String pageType) {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getStatistics(userId, pageType));
    }

    @GetMapping("/project")
    @Operation(summary = "获取项目统计")
    public CommonResult<DashboardStatisticsRespVO.ProjectStats> getProjectStats() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getProjectStats(userId));
    }

    @GetMapping("/contract")
    @Operation(summary = "获取合同统计")
    public CommonResult<DashboardStatisticsRespVO.ContractStats> getContractStats() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getContractStats(userId));
    }

    @GetMapping("/customer")
    @Operation(summary = "获取客户统计")
    public CommonResult<DashboardStatisticsRespVO.CustomerStats> getCustomerStats() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getCustomerStats(userId));
    }

    @GetMapping("/task")
    @Operation(summary = "获取任务统计")
    public CommonResult<DashboardStatisticsRespVO.TaskStats> getTaskStats() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getTaskStats(userId));
    }

    @GetMapping("/revenue")
    @Operation(summary = "获取收入统计")
    public CommonResult<DashboardStatisticsRespVO.RevenueStats> getRevenueStats() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.getRevenueStats(userId));
    }

    @GetMapping("/is-admin")
    @Operation(summary = "判断当前用户是否是管理员")
    public CommonResult<Boolean> isAdmin() {
        Long userId = getLoginUserId();
        return success(dashboardStatisticsService.isAdmin(userId));
    }

}
