package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.service.ProjectSiteService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

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

    @Resource
    private ProjectAccessService projectAccessService;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:create')")
    @DataPermission(enable = false)
    public CommonResult<Long> createSite(@Valid @RequestBody ProjectSiteSaveReqVO createReqVO) {
        validateManageDeptType(createReqVO.getProjectId(), createReqVO.getDeptType());
        return success(siteService.createSite(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateSite(@Valid @RequestBody ProjectSiteSaveReqVO updateReqVO) {
        ProjectSiteDO existing = siteService.getSite(updateReqVO.getId());
        if (existing == null) {
            throwForbidden();
        }
        validateManageDeptType(existing.getProjectId(), existing.getDeptType());
        updateReqVO.setProjectId(existing.getProjectId());
        validateManageDeptType(existing.getProjectId(), updateReqVO.getDeptType());
        siteService.updateSite(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场点")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:site:delete')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> deleteSite(@RequestParam("id") Long id) {
        ProjectSiteDO site = siteService.getSite(id);
        validateManageSite(site);
        siteService.deleteSite(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得驻场点详情")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<ProjectSiteRespVO> getSite(@RequestParam("id") Long id) {
        ProjectSiteDO site = siteService.getSite(id);
        validateReadSite(site);
        return success(siteService.getSiteDetail(id));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID和部门类型获取驻场点列表")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<List<ProjectSiteRespVO>> getListByProjectId(
            @RequestParam("projectId") Long projectId,
            @RequestParam("deptType") Integer deptType) {
        if (!canReadDeptType(projectId, deptType)) {
            return success(List.of());
        }
        return success(siteService.getSiteDetailListByProjectIdAndDeptType(projectId, deptType));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新驻场点状态")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                               @RequestParam("status") Integer status) {
        ProjectSiteDO site = siteService.getSite(id);
        validateManageSite(site);
        siteService.updateStatus(id, status);
        return success(true);
    }

    @GetMapping("/has-site")
    @Operation(summary = "判断项目是否有驻场点")
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> hasSite(@RequestParam("projectId") Long projectId,
                                          @RequestParam("deptType") Integer deptType) {
        if (!canReadDeptType(projectId, deptType)) {
            return success(false);
        }
        return success(siteService.hasSite(projectId, deptType));
    }

    private boolean canReadDeptType(Long projectId, Integer deptType) {
        projectAccessService.validateViewProject(projectId, getLoginUserId());
        return containsDeptType(projectAccessService.getReadableDeptIds(projectId, getLoginUserId()), deptType);
    }

    private void validateManageDeptType(Long projectId, Integer deptType) {
        if (!containsDeptType(projectAccessService.getManageableDeptIds(projectId, getLoginUserId()), deptType)) {
            throwForbidden();
        }
    }

    private boolean containsDeptType(List<Long> deptIds, Integer deptType) {
        if (deptIds == null) {
            return true;
        }
        if (deptIds.isEmpty() || deptType == null) {
            return false;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        return deptMap.values().stream().anyMatch(dept -> deptType.equals(dept.getDeptType()));
    }

    private void validateReadSite(ProjectSiteDO site) {
        if (site == null || !canReadDeptType(site.getProjectId(), site.getDeptType())) {
            throwForbidden();
        }
    }

    private void validateManageSite(ProjectSiteDO site) {
        if (site == null) {
            throwForbidden();
        }
        validateManageDeptType(site.getProjectId(), site.getDeptType());
    }

    private void throwForbidden() {
        throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
    }

}
