package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 服务项部门预算查询 Response VO")
@Data
public class ServiceItemDeptBudgetRespVO {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门预算总额（来自合同收入分配），null表示未设置预算", example = "100000.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "已分配给服务项的金额之和", example = "30000.00")
    private BigDecimal usedAmount;

    @Schema(description = "剩余可分配金额，null表示未设置预算（不限制）", example = "70000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "是否已设置部门预算")
    private Boolean budgetSet;

}
