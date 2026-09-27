package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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

    @Schema(description = "已核对授权文件、有效期和网站范围，批准 GolishAI 自动测试")
    private Boolean golishAuthorizationApproved;

    @Schema(description = "审批页面核对过的自动测试申请快照，内容变化时拒绝审批")
    private java.util.Map<String, Object> golishAuthorizationSnapshot;

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "执行人用户 ID 列表（1 个或多个）",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[200, 201, 202]")
    @NotEmpty(message = "执行人列表不能为空")
    private List<Long> executorIds;

    @Schema(description = "接单备注（写入执行人记录与操作日志）")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

}
