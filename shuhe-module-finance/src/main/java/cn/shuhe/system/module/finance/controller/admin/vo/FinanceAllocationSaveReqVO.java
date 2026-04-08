package cn.shuhe.system.module.finance.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 财务分配新增/修改 Request VO")
@Data
public class FinanceAllocationSaveReqVO {

    @Schema(description = "主键（修改时必填）")
    private Long id;

    @Schema(description = "父节点ID")
    private Long parentId;

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "部门服务单ID")
    private Long deptServiceId;

    @Schema(description = "服务项ID")
    private Long serviceItemId;

    @Schema(description = "分配层级", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分配层级不能为空")
    private Integer allocationLevel;

    @Schema(description = "分配类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分配类型不能为空")
    private String allocationType;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门类型")
    private Integer deptType;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分配金额不能为空")
    private BigDecimal allocatedAmount;

    @Schema(description = "备注")
    private String remark;

}
