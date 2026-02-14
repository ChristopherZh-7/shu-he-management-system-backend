package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectReportDO;
import cn.shuhe.system.module.project.service.ProjectReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 项目周报/汇报")
@RestController
@RequestMapping("/project/project-report")
@Validated
@Slf4j
public class ProjectReportController {

    @Resource
    private ProjectReportService projectReportService;

    @PostMapping("/create")
    @Operation(summary = "创建项目周报")
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:create', 'project:team-overview:query')")
    public CommonResult<Long> createReport(@Valid @RequestBody ProjectReportSaveReqVO createReqVO) {
        return success(projectReportService.createReport(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目周报")
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:update', 'project:team-overview:query')")
    public CommonResult<Boolean> updateReport(@Valid @RequestBody ProjectReportSaveReqVO updateReqVO) {
        projectReportService.updateReport(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目周报")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:delete', 'project:team-overview:query')")
    public CommonResult<Boolean> deleteReport(@RequestParam("id") Long id) {
        projectReportService.deleteReport(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取项目周报详情")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:query', 'project:team-overview:query')")
    public CommonResult<ProjectReportRespVO> getReport(@RequestParam("id") Long id) {
        ProjectReportDO report = projectReportService.getReport(id);
        return success(convertToRespVO(report));
    }

    @GetMapping("/get-by-project-week")
    @Operation(summary = "获取某项目某周的周报")
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:query', 'project:team-overview:query')")
    public CommonResult<ProjectReportRespVO> getReportByProjectAndWeek(
            @RequestParam("projectId") Long projectId,
            @RequestParam("year") Integer year,
            @RequestParam("weekNumber") Integer weekNumber) {
        ProjectReportDO report = projectReportService.getReportByProjectAndWeek(projectId, year, weekNumber);
        return success(convertToRespVO(report));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询项目周报")
    @PreAuthorize("@ss.hasAnyPermissions('project:project-report:query', 'project:team-overview:query')")
    public CommonResult<PageResult<ProjectReportRespVO>> getReportPage(@Valid ProjectReportPageReqVO pageReqVO) {
        PageResult<ProjectReportDO> pageResult = projectReportService.getReportPage(pageReqVO);
        return success(new PageResult<>(
                pageResult.getList().stream().map(this::convertToRespVO).toList(),
                pageResult.getTotal()));
    }

    private ProjectReportRespVO convertToRespVO(ProjectReportDO report) {
        if (report == null) {
            return null;
        }
        ProjectReportRespVO respVO = BeanUtils.toBean(report, ProjectReportRespVO.class);
        if (report.getAttachments() != null && !report.getAttachments().isEmpty()) {
            respVO.setAttachments(JSONUtil.toList(report.getAttachments(), String.class));
        }
        return respVO;
    }

}
