package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteSaveReqVO;
import cn.shuhe.system.module.project.service.SecurityOperationSiteService;
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

@Tag(name = "管理后台 - 安全运营驻场点")
@RestController
@RequestMapping("/project/security-operation-site")
@Validated
public class SecurityOperationSiteController {

    @Resource
    private SecurityOperationSiteService siteService;

    @PostMapping("/create")
    @Operation(summary = "创建驻场点")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Long> createSite(@Valid @RequestBody SecurityOperationSiteSaveReqVO createReqVO) {
        return success(siteService.createSite(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新驻场点")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> updateSite(@Valid @RequestBody SecurityOperationSiteSaveReqVO updateReqVO) {
        siteService.updateSite(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除驻场点")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> deleteSite(@RequestParam("id") Long id) {
        siteService.deleteSite(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得驻场点详情")
    @Parameter(name = "id", description = "驻场点ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<SecurityOperationSiteRespVO> getSite(@RequestParam("id") Long id) {
        return success(siteService.getSiteDetail(id));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID获取驻场点列表")
    @Parameter(name = "projectId", description = "项目ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<List<SecurityOperationSiteRespVO>> getListByProjectId(
            @RequestParam("projectId") Long projectId) {
        return success(siteService.getSiteDetailListByProjectId(projectId));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新驻场点状态")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                               @RequestParam("status") Integer status) {
        siteService.updateStatus(id, status);
        return success(true);
    }

}
