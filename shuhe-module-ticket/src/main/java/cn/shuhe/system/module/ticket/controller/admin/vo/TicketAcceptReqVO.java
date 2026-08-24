package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 主管「接单」请求 VO。
 *
 * <p>对应 {@code PUT /ticket/ticket/accept}。前端校验非空仅做体验优化；服务端再次校验
 * {@link cn.shuhe.system.module.ticket.enums.ErrorCodeConstants#TICKET_EXECUTOR_EMPTY}。
 */
@Schema(description = "管理后台 - 工单接单 Request VO")
@Data
public class TicketAcceptReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "兼容历史客户端的执行人列表；新客户端使用主执行人和协作人")
    private List<Long> executorIds;

    @Schema(description = "主执行人 ID", example = "200")
    private Long primaryExecutorId;

    @Schema(description = "协作人 ID 列表", example = "[201, 202]")
    private List<Long> collaboratorIds;

    @Schema(description = "技术审核人 ID", example = "203")
    private Long techReviewerId;

    @Schema(description = "主执行人负责内容")
    @Size(max = 500, message = "负责内容不能超过 500 个字符")
    private String primaryResponsibility;

    @Schema(description = "协作说明")
    @Size(max = 500, message = "协作说明不能超过 500 个字符")
    private String collaboratorResponsibility;

    @Schema(description = "接单备注（写入执行人记录与操作日志）")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

}
