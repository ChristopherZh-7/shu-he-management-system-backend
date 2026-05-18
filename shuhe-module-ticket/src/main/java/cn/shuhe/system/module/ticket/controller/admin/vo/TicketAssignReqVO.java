package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单分派 Request VO")
@Data
public class TicketAssignReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "新处理人 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "处理人不能为空")
    private Long assigneeId;

    @Schema(description = "分派说明（写入操作日志）")
    @Size(max = 500, message = "分派说明不能超过 500 个字符")
    private String remark;

}
