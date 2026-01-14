package cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 钉钉数据映射新增/修改 Request VO")
@Data
public class DingtalkMappingSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "5104")
    private Long id;

    @Schema(description = "钉钉配置ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "31125")
    @NotNull(message = "钉钉配置ID不能为空")
    private Long configId;

    @Schema(description = "类型（DEPT-部门，USER-用户）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotEmpty(message = "类型（DEPT-部门，USER-用户）不能为空")
    private String type;

    @Schema(description = "本地ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3825")
    @NotNull(message = "本地ID不能为空")
    private Long localId;

    @Schema(description = "钉钉ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10379")
    @NotEmpty(message = "钉钉ID不能为空")
    private String dingtalkId;

}