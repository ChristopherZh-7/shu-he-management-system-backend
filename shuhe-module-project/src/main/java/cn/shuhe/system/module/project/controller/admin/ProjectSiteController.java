package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteSaveReqVO;
import cn.shuhe.system.module.project.service.ProjectSiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 项目驻场点
 * 
 * 通用的驻场点管理，支持所有部门类型的项目
 */
@Tag(name = "管理后台 - 项目驻场点")
@RestController
@RequestMapping("/project/site")
@Validated
public class ProjectSiteController {

    @Resource
    private ProjectSiteService siteService;

    @PostMapping("/create")
    @Operation(summary = "创建驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:create')")
    public CommonResult<Long> createSite(@Valid @RequestBody ProjectSiteSaveReqVO createReqVO) {
        return success(siteService.createSite(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Boolean> updateSite(@Valid @RequestBody ProjectSiteSaveReqVO updateReqVO) {
        siteService.updateSite(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场点")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:site:delete')")
    public CommonResult<Boolean> deleteSite(@RequestParam("id") Long id) {
        siteService.deleteSite(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得驻场点详情")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<ProjectSiteRespVO> getSite(@RequestParam("id") Long id) {
        return success(siteService.getSiteDetail(id));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID和部门类型获取驻场点列表")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<List<ProjectSiteRespVO>> getListByProjectId(
            @RequestParam("projectId") Long projectId,
            @RequestParam("deptType") Integer deptType) {
        return success(siteService.getSiteDetailListByProjectIdAndDeptType(projectId, deptType));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新驻场点状态")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                               @RequestParam("status") Integer status) {
        siteService.updateStatus(id, status);
        return success(true);
    }

    @GetMapping("/has-site")
    @Operation(summary = "判断项目是否有驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<Boolean> hasSite(@RequestParam("projectId") Long projectId,
                                          @RequestParam("deptType") Integer deptType) {
        return success(siteService.hasSite(projectId, deptType));
    }

}
