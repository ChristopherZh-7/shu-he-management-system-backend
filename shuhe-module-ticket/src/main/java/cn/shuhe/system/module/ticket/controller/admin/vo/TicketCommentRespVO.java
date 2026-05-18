package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单评论 Response VO")
@Data
public class TicketCommentRespVO {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "工单 ID")
    private Long ticketId;

    @Schema(description = "评论人 ID")
    private Long userId;

    @Schema(description = "评论人姓名快照")
    private String userName;

    @Schema(description = "评论人部门 ID 快照")
    private Long userDeptId;

    @Schema(description = "回复的父评论 ID")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "是否内部评论")
    private Boolean isInternal;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
