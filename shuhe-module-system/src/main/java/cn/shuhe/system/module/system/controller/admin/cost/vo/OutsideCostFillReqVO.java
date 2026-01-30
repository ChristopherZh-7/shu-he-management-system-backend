package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 填写外出费用金额 Request VO")
@Data
public class OutsideCostFillReqVO {

    @Schema(description = "外出费用记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "外出费用记录ID不能为空")
    private Long id;

    @Schema(description = "费用金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "费用金额不能为空")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;
}
