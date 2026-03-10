package cn.shuhe.system.module.crm.controller.admin.resignation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 离职交接 - 执行 Request VO
 *
 * @author ShuHe
 */
@Schema(description = "管理后台 - 离职交接执行 Request VO")
@Data
public class ResignationHandoverExecuteReqVO {

    @Schema(description = "离职用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "离职用户不能为空")
    private Long resignUserId;

    @Schema(description = "接任用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "接任用户不能为空")
    private Long newOwnerUserId;
}
