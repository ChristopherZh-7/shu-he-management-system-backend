package cn.shuhe.system.module.crm.controller.admin.resignation;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverExecuteReqVO;
import cn.shuhe.system.module.crm.controller.admin.resignation.vo.ResignationHandoverPreviewRespVO;
import cn.shuhe.system.module.crm.service.resignation.ResignationHandoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 离职交接 Controller
 *
 * @author ShuHe
 */
@Tag(name = "管理后台 - 离职交接")
@RestController
@RequestMapping("/system/resignation-handover")
@Validated
public class ResignationHandoverController {

    @Resource
    private ResignationHandoverService resignationHandoverService;

    @GetMapping("/preview")
    @Operation(summary = "预览离职交接数据")
    @Parameter(name = "resignUserId", description = "离职用户ID", required = true, example = "100")
    @Parameter(name = "newOwnerUserId", description = "接任用户ID", required = true, example = "101")
    @PreAuthorize("@ss.hasPermission('system:resignation-handover:query')")
    public CommonResult<ResignationHandoverPreviewRespVO> preview(
            @RequestParam("resignUserId") Long resignUserId,
            @RequestParam("newOwnerUserId") Long newOwnerUserId) {
        return success(resignationHandoverService.preview(resignUserId, newOwnerUserId));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行离职交接")
    @PreAuthorize("@ss.hasPermission('system:resignation-handover:execute')")
    public CommonResult<Boolean> execute(@Valid @RequestBody ResignationHandoverExecuteReqVO reqVO) {
        resignationHandoverService.execute(reqVO, getLoginUserId());
        return success(true);
    }
}
