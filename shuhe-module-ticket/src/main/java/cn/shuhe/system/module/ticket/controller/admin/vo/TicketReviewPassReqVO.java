package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单验收通过 Request VO（status 2 → 3，仅提单人/超管）")
@Data
public class TicketReviewPassReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "验收意见（可选）")
    @Size(max = 500, message = "验收意见不能超过 500 个字符")
    private String comment;

}
