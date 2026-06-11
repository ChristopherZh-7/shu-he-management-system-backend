package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单重开 Request VO（status 3/4 → 1；窗口期与次数限制见 Service）")
@Data
public class TicketReopenReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "重开原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "重开原因不能为空")
    @Size(max = 500, message = "重开原因不能超过 500 个字符")
    private String reason;

}
