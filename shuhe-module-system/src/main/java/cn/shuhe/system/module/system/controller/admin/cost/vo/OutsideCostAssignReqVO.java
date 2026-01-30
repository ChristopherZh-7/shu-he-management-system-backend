package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 指派结算人 Request VO")
@Data
public class OutsideCostAssignReqVO {

    @Schema(description = "外出费用记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "外出费用记录ID不能为空")
    private Long id;

    @Schema(description = "结算人ID（找谁要钱）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结算人不能为空")
    private Long settleUserId;
}
