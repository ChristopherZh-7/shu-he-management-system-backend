package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 轮次测试目标创建/修改 Request VO")
@Data
public class ProjectRoundTargetSaveReqVO {

    @Schema(description = "目标ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "轮次ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "轮次ID不能为空")
    private Long roundId;

    @Schema(description = "目标名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "官网系统")
    @NotBlank(message = "目标名称不能为空")
    private String name;

    @Schema(description = "目标地址/URL", example = "https://www.example.com")
    private String url;

    @Schema(description = "目标类型", example = "web")
    private String type;

    @Schema(description = "目标描述", example = "公司官网系统")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
