package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单评论保存 Request VO")
@Data
public class TicketCommentSaveReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long ticketId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论内容不能超过 5000 个字符")
    private String content;

    @Schema(description = "回复的父评论 ID（可选）")
    private Long parentId;

    @Schema(description = "是否内部评论：仅处理人 / 管理员可创建；提单人看不到")
    private Boolean isInternal;

}
