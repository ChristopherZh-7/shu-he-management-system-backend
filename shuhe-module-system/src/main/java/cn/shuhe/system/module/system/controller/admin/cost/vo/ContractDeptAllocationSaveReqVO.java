package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 合同部门分配新增/修改 Request VO
 */
@Schema(description = "管理后台 - 合同部门分配新增/修改 Request VO")
@Data
public class ContractDeptAllocationSaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "CRM合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "240000.00")
    @NotNull(message = "分配金额不能为空")
    private BigDecimal allocatedAmount;

    @Schema(description = "备注", example = "安全服务部份额")
    private String remark;

}
