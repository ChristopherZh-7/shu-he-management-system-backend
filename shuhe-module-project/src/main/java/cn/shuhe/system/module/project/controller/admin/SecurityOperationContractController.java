package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.*;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import cn.shuhe.system.module.project.service.SecurityOperationContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 安全运营
 */
@Tag(name = "管理后台 - 安全运营")
@RestController
@RequestMapping("/project/security-operation")
@Validated
public class SecurityOperationContractController {

    @Resource
    private SecurityOperationContractService securityOperationContractService;

    @PostMapping("/create")
    @Operation(summary = "创建安全运营合同")
    @PreAuthorize("@ss.hasPermission('project:security-operation:create')")
    public CommonResult<Long> createSecurityOperationContract(@Valid @RequestBody SecurityOperationContractSaveReqVO createReqVO) {
        return success(securityOperationContractService.createSecurityOperationContract(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新安全运营合同")
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> updateSecurityOperationContract(@Valid @RequestBody SecurityOperationContractSaveReqVO updateReqVO) {
        securityOperationContractService.updateSecurityOperationContract(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除安全运营合同")
    @Parameter(name = "id", description = "安全运营合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:delete')")
    public CommonResult<Boolean> deleteSecurityOperationContract(@RequestParam("id") Long id) {
        securityOperationContractService.deleteSecurityOperationContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取安全运营合同详情")
    @Parameter(name = "id", description = "安全运营合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<SecurityOperationContractRespVO> getSecurityOperationContract(@RequestParam("id") Long id) {
        return success(securityOperationContractService.getSecurityOperationContractDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取安全运营合同分页")
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<PageResult<SecurityOperationContractRespVO>> getSecurityOperationContractPage(@Valid SecurityOperationContractPageReqVO pageReqVO) {
        PageResult<SecurityOperationContractDO> pageResult = securityOperationContractService.getSecurityOperationContractPage(pageReqVO);
        
        // 转换为响应VO
        PageResult<SecurityOperationContractRespVO> result = new PageResult<>();
        result.setTotal(pageResult.getTotal());
        result.setList(pageResult.getList().stream().map(contract -> {
            SecurityOperationContractRespVO respVO = BeanUtils.toBean(contract, SecurityOperationContractRespVO.class);
            // 计算总费用
            BigDecimal managementFee = contract.getManagementFee() != null ? contract.getManagementFee() : BigDecimal.ZERO;
            BigDecimal onsiteFee = contract.getOnsiteFee() != null ? contract.getOnsiteFee() : BigDecimal.ZERO;
            respVO.setTotalFee(managementFee.add(onsiteFee));
            return respVO;
        }).toList());
        
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获取安全运营合同列表（简单列表，用于下拉选择）")
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<List<SecurityOperationContractRespVO>> getSecurityOperationContractList() {
        SecurityOperationContractPageReqVO pageReqVO = new SecurityOperationContractPageReqVO();
        pageReqVO.setPageSize(1000); // 获取所有
        PageResult<SecurityOperationContractDO> pageResult = securityOperationContractService.getSecurityOperationContractPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult.getList(), SecurityOperationContractRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新安全运营合同状态")
    @Parameter(name = "id", description = "安全运营合同ID", required = true)
    @Parameter(name = "status", description = "状态：0-待启动 1-进行中 2-已结束 3-已终止", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:update')")
    public CommonResult<Boolean> updateSecurityOperationContractStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        securityOperationContractService.updateStatus(id, status);
        return success(true);
    }

    @GetMapping("/by-contract")
    @Operation(summary = "根据合同ID获取安全运营合同")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<SecurityOperationContractRespVO> getByContractId(@RequestParam("contractId") Long contractId) {
        SecurityOperationContractDO contract = securityOperationContractService.getByContractId(contractId);
        if (contract == null) {
            return success(null);
        }
        return success(securityOperationContractService.getSecurityOperationContractDetail(contract.getId()));
    }

    @GetMapping("/by-customer")
    @Operation(summary = "根据客户ID获取安全运营合同列表")
    @Parameter(name = "customerId", description = "客户ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:security-operation:query')")
    public CommonResult<List<SecurityOperationContractRespVO>> getByCustomerId(@RequestParam("customerId") Long customerId) {
        List<SecurityOperationContractDO> contracts = securityOperationContractService.getListByCustomerId(customerId);
        return success(BeanUtils.toBean(contracts, SecurityOperationContractRespVO.class));
    }

}
