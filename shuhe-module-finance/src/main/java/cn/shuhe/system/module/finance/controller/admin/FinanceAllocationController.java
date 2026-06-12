package cn.shuhe.system.module.finance.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationRespVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceAllocationSaveReqVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceContractSummaryVO;
import cn.shuhe.system.module.finance.controller.admin.vo.FinanceInitAllocationReqVO;
import cn.shuhe.system.module.finance.service.FinanceAllocationService;
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

@Tag(name = "管理后台 - 财务分配管理")
@RestController
@RequestMapping("/finance/allocation")
@Validated
public class FinanceAllocationController {

    @Resource
    private FinanceAllocationService allocationService;

    @GetMapping("/contract-summary")
    @Operation(summary = "获取合同财务概览列表")
    @PreAuthorize("@ss.hasPermission('finance:project-budget:query')")
    public CommonResult<List<FinanceContractSummaryVO>> getContractSummaryList() {
        return success(allocationService.getContractSummaryList());
    }

    @PostMapping("/create")
    @Operation(summary = "创建分配节点")
    @PreAuthorize("@ss.hasPermission('finance:project-budget:create')")
    public CommonResult<Long> createAllocation(@Valid @RequestBody FinanceAllocationSaveReqVO createReqVO) {
        return success(allocationService.createAllocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新分配节点")
    @PreAuthorize("@ss.hasPermission('finance:project-budget:update')")
    public CommonResult<Boolean> updateAllocation(@Valid @RequestBody FinanceAllocationSaveReqVO updateReqVO) {
        allocationService.updateAllocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除分配节点")
    @Parameter(name = "id", description = "分配节点ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:project-budget:delete')")
    public CommonResult<Boolean> deleteAllocation(@RequestParam("id") Long id) {
        allocationService.deleteAllocation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取分配节点详情")
    @Parameter(name = "id", description = "分配节点ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:project-budget:query')")
    public CommonResult<FinanceAllocationRespVO> getAllocation(@RequestParam("id") Long id) {
        return success(null);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取合同分配树")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:project-budget:query')")
    public CommonResult<List<FinanceAllocationRespVO>> getAllocationTree(@RequestParam("contractId") Long contractId) {
        return success(allocationService.getAllocationTree(contractId));
    }

    @GetMapping("/children")
    @Operation(summary = "获取子节点列表")
    @Parameter(name = "parentId", description = "父节点ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:project-budget:query')")
    public CommonResult<List<FinanceAllocationRespVO>> getChildAllocations(@RequestParam("parentId") Long parentId) {
        return success(allocationService.getChildAllocations(parentId));
    }

    @PostMapping("/init-from-contract")
    @Operation(summary = "从合同初始化分配")
    @PreAuthorize("@ss.hasPermission('finance:project-budget:create')")
    public CommonResult<Boolean> initFromContract(@Valid @RequestBody FinanceInitAllocationReqVO reqVO) {
        allocationService.initAllocationsFromContract(reqVO);
        return success(true);
    }

    @PostMapping("/auto-init")
    @Operation(summary = "自动从合同初始化分配")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('finance:project-budget:create')")
    public CommonResult<Boolean> autoInitFromContract(@RequestParam("contractId") Long contractId) {
        allocationService.autoInitAllocationsFromContract(contractId);
        return success(true);
    }

}
