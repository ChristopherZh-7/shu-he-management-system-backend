package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 管理后台 - 服务项金额分配新增/修改 Request VO
 */
@Schema(description = "管理后台 - 服务项金额分配新增/修改 Request VO")
@Data
public class ServiceItemAllocationSaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "合同部门分配ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "合同部门分配ID不能为空")
    private Long contractDeptAllocationId;

    @Schema(description = "分配类型：service_item-服务项分配, so_management-安全运营管理费, so_onsite-安全运营驻场费", example = "service_item")
    private String allocationType;

    @Schema(description = "服务项ID（服务项分配时必填）", example = "200")
    private Long serviceItemId;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50000.00")
    @NotNull(message = "分配金额不能为空")
    private BigDecimal allocatedAmount;

    @Schema(description = "备注", example = "渗透测试服务费")
    private String remark;

}
