package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 管理后台 - 分配给下级部门 Request VO
 */
@Schema(description = "管理后台 - 分配给下级部门 Request VO")
@Data
public class ContractAllocationDistributeReqVO {

    @Schema(description = "当前部门的分配记录ID（作为上级）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "上级分配ID不能为空")
    private Long parentAllocationId;

    @Schema(description = "目标子部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "子部门ID不能为空")
    private Long childDeptId;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    @NotNull(message = "分配金额不能为空")
    @DecimalMin(value = "0.01", message = "分配金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "备注", example = "分配给1营")
    private String remark;

}
