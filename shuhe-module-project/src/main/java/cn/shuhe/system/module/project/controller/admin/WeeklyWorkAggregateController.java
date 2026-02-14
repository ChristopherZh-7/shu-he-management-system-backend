package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.TeamOverviewRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateRespVO;
import cn.shuhe.system.module.project.service.WeeklyWorkAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 周工作聚合 Controller
 * 聚合日常管理记录和项目管理记录，提供周视图展示
 */
@Tag(name = "管理后台 - 周工作聚合视图")
@RestController
@RequestMapping("/project/weekly-work-aggregate")
@Validated
@Slf4j
public class WeeklyWorkAggregateController {

    @Resource
    private WeeklyWorkAggregateService weeklyWorkAggregateService;

    @GetMapping("/get")
    @Operation(summary = "获取指定周的工作聚合数据", description = "聚合日常管理记录和项目管理记录，按日期展示")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<WeeklyWorkAggregateRespVO> getWeeklyAggregate(@Valid WeeklyWorkAggregateReqVO reqVO) {
        return success(weeklyWorkAggregateService.getWeeklyWorkAggregate(reqVO));
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前周的工作聚合数据", description = "获取当前登录用户本周的工作聚合数据")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<WeeklyWorkAggregateRespVO> getCurrentWeekAggregate() {
        return success(weeklyWorkAggregateService.getCurrentWeekAggregate());
    }

    @GetMapping("/viewable-users")
    @Operation(summary = "获取可查看的员工列表", description = "领导可以查看自己和下属员工，普通员工只能查看自己")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<List<Map<String, Object>>> getViewableUserList() {
        return success(weeklyWorkAggregateService.getViewableUserList());
    }

    @GetMapping("/team-overview")
    @Operation(summary = "获取团队工作总览", description = "管理层专用，查看所有下属某周的工作汇总")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<TeamOverviewRespVO> getTeamOverview(
            @RequestParam("year") Integer year,
            @RequestParam("weekNumber") Integer weekNumber) {
        return success(weeklyWorkAggregateService.getTeamOverview(year, weekNumber));
    }

    @GetMapping("/work-mode")
    @Operation(summary = "获取当前用户的工作模式", description = "返回1-驻场 2-二线")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<Integer> getCurrentUserWorkMode() {
        return success(weeklyWorkAggregateService.getCurrentUserWorkMode());
    }

    @GetMapping("/work-mode-info")
    @Operation(summary = "获取当前用户的工作模式及驻场项目信息", description = "返回workMode和驻场项目详情")
    @PreAuthorize("@ss.hasAnyPermissions('project:daily-record:query', 'project:work-record:query', 'project:management-record:query', 'project:my-work-record:query', 'project:team-overview:query')")
    public CommonResult<Map<String, Object>> getCurrentUserWorkModeInfo() {
        return success(weeklyWorkAggregateService.getCurrentUserWorkModeInfo());
    }

}
