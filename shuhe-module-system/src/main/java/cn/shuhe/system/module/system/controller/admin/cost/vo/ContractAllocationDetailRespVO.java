package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 合同分配详情 Response VO
 * 
 * 包含合同信息 + 所有部门分配
 */
@Schema(description = "管理后台 - 合同分配详情 Response VO")
@Data
public class ContractAllocationDetailRespVO {

    @Schema(description = "CRM合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long contractId;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "合同名称", example = "2026年安全服务项目")
    private String contractName;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "合同总金额", example = "500000.00")
    private BigDecimal totalAmount;

    @Schema(description = "已分配给部门的金额", example = "400000.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "剩余可分配金额", example = "100000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "部门分配列表")
    private List<ContractDeptAllocationRespVO> deptAllocations;

}
