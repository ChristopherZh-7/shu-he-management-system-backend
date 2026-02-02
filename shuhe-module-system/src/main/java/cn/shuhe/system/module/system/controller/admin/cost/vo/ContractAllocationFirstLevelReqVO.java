package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 管理后台 - 第一级分配（合同直接分配给一级部门） Request VO
 */
@Schema(description = "管理后台 - 第一级分配 Request VO")
@Data
public class ContractAllocationFirstLevelReqVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "目标一级部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "500000.00")
    @NotNull(message = "分配金额不能为空")
    @DecimalMin(value = "0.01", message = "分配金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "备注", example = "安服部份额")
    private String remark;

}
