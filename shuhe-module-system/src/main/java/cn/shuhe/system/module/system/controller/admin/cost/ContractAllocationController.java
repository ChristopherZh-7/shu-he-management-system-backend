package cn.shuhe.system.module.system.controller.admin.cost;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;
import cn.shuhe.system.module.system.dal.mysql.cost.ContractInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemInfoMapper;
import cn.shuhe.system.module.system.service.cost.ContractAllocationService;
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

/**
 * 管理后台 - 合同收入分配
 */
@Tag(name = "管理后台 - 合同收入分配")
@RestController
@RequestMapping("/system/contract-allocation")
@Validated
public class ContractAllocationController {

    @Resource
    private ContractAllocationService contractAllocationService;

    @Resource
    private ContractInfoMapper contractInfoMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    // ========== 合同查询接口 ==========

    @GetMapping("/contract/list")
    @Operation(summary = "获取可分配的合同列表")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<List<Map<String, Object>>> getContractList() {
        return success(contractInfoMapper.selectContractList());
    }

    @GetMapping("/contract/search")
    @Operation(summary = "搜索合同")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<List<Map<String, Object>>> searchContracts(
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String customerName) {
        return success(contractInfoMapper.searchContracts(contractNo, customerName));
    }

    @GetMapping("/contract/detail")
    @Operation(summary = "获取合同分配详情")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<ContractAllocationDetailRespVO> getContractAllocationDetail(@RequestParam Long contractId) {
        return success(contractAllocationService.getContractAllocationDetail(contractId));
    }

    // ========== 合同部门分配接口 ==========

    @PostMapping("/dept/create")
    @Operation(summary = "创建合同部门分配")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:create')")
    public CommonResult<Long> createContractDeptAllocation(@Valid @RequestBody ContractDeptAllocationSaveReqVO reqVO) {
        return success(contractAllocationService.createContractDeptAllocation(reqVO));
    }

    @PutMapping("/dept/update")
    @Operation(summary = "更新合同部门分配")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:update')")
    public CommonResult<Boolean> updateContractDeptAllocation(@Valid @RequestBody ContractDeptAllocationSaveReqVO reqVO) {
        contractAllocationService.updateContractDeptAllocation(reqVO);
        return success(true);
    }

    @DeleteMapping("/dept/delete")
    @Operation(summary = "删除合同部门分配")
    @Parameter(name = "id", description = "分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:delete')")
    public CommonResult<Boolean> deleteContractDeptAllocation(@RequestParam Long id) {
        contractAllocationService.deleteContractDeptAllocation(id);
        return success(true);
    }

    @GetMapping("/dept/get")
    @Operation(summary = "获取合同部门分配详情")
    @Parameter(name = "id", description = "分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<ContractDeptAllocationRespVO> getContractDeptAllocation(@RequestParam Long id) {
        return success(contractAllocationService.getContractDeptAllocation(id));
    }

    @GetMapping("/dept/page")
    @Operation(summary = "分页查询合同部门分配")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<PageResult<ContractDeptAllocationRespVO>> getContractDeptAllocationPage(@Valid ContractDeptAllocationPageReqVO reqVO) {
        return success(contractAllocationService.getContractDeptAllocationPage(reqVO));
    }

    @GetMapping("/dept/list-by-dept")
    @Operation(summary = "获取部门的分配列表（部门视图）")
    @Parameter(name = "deptId", description = "部门ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<List<ContractDeptAllocationRespVO>> getContractDeptAllocationsByDeptId(@RequestParam Long deptId) {
        return success(contractAllocationService.getContractDeptAllocationsByDeptId(deptId));
    }

    // ========== 服务项分配接口 ==========

    @GetMapping("/service-item/list")
    @Operation(summary = "获取合同下可分配的服务项列表")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<List<Map<String, Object>>> getServiceItemsByContract(
            @RequestParam Long contractId,
            @RequestParam(required = false) Long deptId) {
        return success(serviceItemInfoMapper.selectServiceItemsByContractAndDept(contractId, deptId));
    }

    @PostMapping("/service-item/create")
    @Operation(summary = "创建服务项金额分配")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:create')")
    public CommonResult<Long> createServiceItemAllocation(@Valid @RequestBody ServiceItemAllocationSaveReqVO reqVO) {
        return success(contractAllocationService.createServiceItemAllocation(reqVO));
    }

    @PutMapping("/service-item/update")
    @Operation(summary = "更新服务项金额分配")
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:update')")
    public CommonResult<Boolean> updateServiceItemAllocation(@Valid @RequestBody ServiceItemAllocationSaveReqVO reqVO) {
        contractAllocationService.updateServiceItemAllocation(reqVO);
        return success(true);
    }

    @DeleteMapping("/service-item/delete")
    @Operation(summary = "删除服务项金额分配")
    @Parameter(name = "id", description = "分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:delete')")
    public CommonResult<Boolean> deleteServiceItemAllocation(@RequestParam Long id) {
        contractAllocationService.deleteServiceItemAllocation(id);
        return success(true);
    }

    @GetMapping("/service-item/get")
    @Operation(summary = "获取服务项金额分配详情")
    @Parameter(name = "id", description = "分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<ServiceItemAllocationRespVO> getServiceItemAllocation(@RequestParam Long id) {
        return success(contractAllocationService.getServiceItemAllocation(id));
    }

    @GetMapping("/service-item/list-by-dept-allocation")
    @Operation(summary = "获取部门分配下的服务项分配列表")
    @Parameter(name = "contractDeptAllocationId", description = "合同部门分配ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:contract-allocation:query')")
    public CommonResult<List<ServiceItemAllocationRespVO>> getServiceItemAllocationsByDeptAllocation(
            @RequestParam Long contractDeptAllocationId) {
        return success(contractAllocationService.getServiceItemAllocationsByDeptAllocationId(contractDeptAllocationId));
    }

}
