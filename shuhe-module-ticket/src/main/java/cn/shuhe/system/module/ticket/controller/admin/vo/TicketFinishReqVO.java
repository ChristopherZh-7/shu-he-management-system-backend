package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单完成 Request VO")
@Data
public class TicketFinishReqVO {

    @Schema(description = "工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工单 ID 不能为空")
    private Long id;

    @Schema(description = "处理结果说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理结果说明不能为空")
    @Size(max = 1000, message = "处理结果说明不能超过 1000 个字符")
    private String result;

}
