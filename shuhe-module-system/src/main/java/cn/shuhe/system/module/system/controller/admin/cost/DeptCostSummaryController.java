package cn.shuhe.system.module.system.controller.admin.cost;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryRespVO;
import cn.shuhe.system.module.system.service.cost.DeptCostSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 部门费用汇总 Controller
 */
@Tag(name = "管理后台 - 部门费用汇总")
@RestController
@RequestMapping("/system/dept-cost-summary")
@Validated
public class DeptCostSummaryController {

    @Resource
    private DeptCostSummaryService deptCostSummaryService;

    @GetMapping("/get")
    @Operation(summary = "获取部门费用汇总")
    @PreAuthorize("@ss.hasPermission('system:cost:query')")
    public CommonResult<DeptCostSummaryRespVO> getDeptCostSummary(DeptCostSummaryReqVO reqVO) {
        return success(deptCostSummaryService.getDeptCostSummary(reqVO));
    }

}
