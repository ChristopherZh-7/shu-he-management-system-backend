package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.service.ProjectSiteMemberService;
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
 * 管理后台 - 项目驻场人员
 */
@Tag(name = "管理后台 - 项目驻场人员")
@RestController
@RequestMapping("/project/site-member")
@Validated
public class ProjectSiteMemberController {

    @Resource
    private ProjectSiteMemberService memberService;

    @Resource
    private ProjectSiteService siteService;

    @Resource
    private ProjectAccessService projectAccessService;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建驻场人员")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Long> createMember(@Valid @RequestBody ProjectSiteMemberSaveReqVO createReqVO) {
        validateManageSite(siteService.getSite(createReqVO.getSiteId()));
        return success(memberService.createMember(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场人员")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> updateMember(@Valid @RequestBody ProjectSiteMemberSaveReqVO updateReqVO) {
        ProjectSiteMemberDO existing = memberService.getMember(updateReqVO.getId());
        validateManageMember(existing);
        validateManageSite(siteService.getSite(updateReqVO.getSiteId()));
        memberService.updateMember(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场人员")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> deleteMember(@RequestParam("id") Long id) {
        validateManageMember(memberService.getMember(id));
        memberService.deleteMember(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取驻场人员详情")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<ProjectSiteMemberRespVO> getMember(@RequestParam("id") Long id) {
        validateReadMember(memberService.getMember(id));
        return success(memberService.getMemberDetail(id));
    }

    @GetMapping("/list-by-site")
    @Operation(summary = "获取驻场点的人员列表")
    @Parameter(name = "siteId", description = "驻场点ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<List<ProjectSiteMemberRespVO>> getListBySite(@RequestParam("siteId") Long siteId) {
        validateReadSite(siteService.getSite(siteId));
        return success(memberService.getListBySiteId(siteId));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "获取项目的所有驻场人员")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    @DataPermission(enable = false)
    public CommonResult<List<ProjectSiteMemberRespVO>> getListByProject(@RequestParam("projectId") Long projectId) {
        projectAccessService.validateViewProject(projectId, getLoginUserId());
        List<Long> readableDeptIds = projectAccessService.getReadableDeptIds(projectId, getLoginUserId());
        List<ProjectSiteMemberRespVO> result = memberService.getListByProjectId(projectId);
        if (readableDeptIds == null) {
            return success(result);
        }
        if (readableDeptIds.isEmpty()) {
            return success(List.of());
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(readableDeptIds);
        return success(result.stream()
                .filter(member -> containsDeptType(deptMap, member.getDeptType()))
                .toList());
    }

    @PutMapping("/set-left")
    @Operation(summary = "标记人员已离开")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    @DataPermission(enable = false)
    public CommonResult<Boolean> setMemberLeft(@RequestParam("id") Long id) {
        validateManageMember(memberService.getMember(id));
        memberService.setMemberLeft(id);
        return success(true);
    }

    private void validateReadSite(ProjectSiteDO site) {
        if (site == null || !canReadDeptType(site.getProjectId(), site.getDeptType())) {
            throwForbidden();
        }
    }

    private void validateManageSite(ProjectSiteDO site) {
        if (site == null || !canManageDeptType(site.getProjectId(), site.getDeptType())) {
            throwForbidden();
        }
    }

    private void validateReadMember(ProjectSiteMemberDO member) {
        if (member == null || !canReadDeptType(member.getProjectId(), member.getDeptType())) {
            throwForbidden();
        }
    }

    private void validateManageMember(ProjectSiteMemberDO member) {
        if (member == null || !canManageDeptType(member.getProjectId(), member.getDeptType())) {
            throwForbidden();
        }
    }

    private boolean canReadDeptType(Long projectId, Integer deptType) {
        if (!projectAccessService.canViewProject(projectId, getLoginUserId())) {
            return false;
        }
        return containsDeptType(projectAccessService.getReadableDeptIds(projectId, getLoginUserId()), deptType);
    }

    private boolean canManageDeptType(Long projectId, Integer deptType) {
        return containsDeptType(projectAccessService.getManageableDeptIds(projectId, getLoginUserId()), deptType);
    }

    private boolean containsDeptType(List<Long> deptIds, Integer deptType) {
        if (deptIds == null) {
            return true;
        }
        if (deptIds.isEmpty() || deptType == null) {
            return false;
        }
        return containsDeptType(deptApi.getDeptMap(deptIds), deptType);
    }

    private boolean containsDeptType(Map<Long, DeptRespDTO> deptMap, Integer deptType) {
        return deptType != null && deptMap.values().stream()
                .anyMatch(dept -> deptType.equals(dept.getDeptType()));
    }

    private void throwForbidden() {
        throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
    }

}
