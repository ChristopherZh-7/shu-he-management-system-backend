package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工单创建/更新 Request VO")
@Data
public class TicketSaveReqVO {

    @Schema(description = "工单ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "网络故障")
    @NotBlank(message = "工单标题不能为空")
    private String title;

    @Schema(description = "工单描述", example = "客户反映网络无法连接")
    private String description;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "优先级：0-低 1-普通 2-高 3-紧急", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @Schema(description = "关联客户ID", example = "1")
    private Long customerId;

    @Schema(description = "期望完成时间")
    private LocalDateTime expectTime;

    @Schema(description = "附件URL（JSON数组）")
    private String attachments;

    @Schema(description = "备注")
    private String remark;

}
