package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 工单分类创建/更新 Request VO")
@Data
public class CategorySaveReqVO {

    @Schema(description = "分类ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "IT支持")
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "父分类ID", example = "0")
    private Long parentId;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-启用", example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
