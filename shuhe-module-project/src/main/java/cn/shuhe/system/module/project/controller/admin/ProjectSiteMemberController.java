package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberSaveReqVO;
import cn.shuhe.system.module.project.service.ProjectSiteMemberService;
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
 * 管理后台 - 项目驻场人员
 */
@Tag(name = "管理后台 - 项目驻场人员")
@RestController
@RequestMapping("/project/site-member")
@Validated
public class ProjectSiteMemberController {

    @Resource
    private ProjectSiteMemberService memberService;

    @PostMapping("/create")
    @Operation(summary = "创建驻场人员")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Long> createMember(@Valid @RequestBody ProjectSiteMemberSaveReqVO createReqVO) {
        return success(memberService.createMember(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场人员")
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Boolean> updateMember(@Valid @RequestBody ProjectSiteMemberSaveReqVO updateReqVO) {
        memberService.updateMember(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场人员")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Boolean> deleteMember(@RequestParam("id") Long id) {
        memberService.deleteMember(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取驻场人员详情")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<ProjectSiteMemberRespVO> getMember(@RequestParam("id") Long id) {
        return success(memberService.getMemberDetail(id));
    }

    @GetMapping("/list-by-site")
    @Operation(summary = "获取驻场点的人员列表")
    @Parameter(name = "siteId", description = "驻场点ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<List<ProjectSiteMemberRespVO>> getListBySite(@RequestParam("siteId") Long siteId) {
        return success(memberService.getListBySiteId(siteId));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "获取项目的所有驻场人员")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:query')")
    public CommonResult<List<ProjectSiteMemberRespVO>> getListByProject(@RequestParam("projectId") Long projectId) {
        return success(memberService.getListByProjectId(projectId));
    }

    @PutMapping("/set-left")
    @Operation(summary = "标记人员已离开")
    @Parameter(name = "id", description = "人员ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:site:update')")
    public CommonResult<Boolean> setMemberLeft(@RequestParam("id") Long id) {
        memberService.setMemberLeft(id);
        return success(true);
    }

}
