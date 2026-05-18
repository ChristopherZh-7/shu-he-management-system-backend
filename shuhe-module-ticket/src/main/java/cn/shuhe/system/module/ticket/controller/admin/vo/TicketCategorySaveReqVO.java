package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 工单分类保存 Request VO")
@Data
public class TicketCategorySaveReqVO {

    @Schema(description = "分类 ID，新增时不传")
    private Long id;

    @Schema(description = "父分类 ID，0=顶级", example = "0")
    private Long parentId;

    @Schema(description = "分类名称（同 parentId 下唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "IT 支持")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过 100 个字符")
    private String name;

    @Schema(description = "分类编码（全局唯一，可选）")
    @Size(max = 50, message = "分类编码不能超过 50 个字符")
    private String code;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "默认处理人 ID")
    private Long defaultAssigneeId;

    @Schema(description = "默认处理部门 ID")
    private Long defaultAssigneeDeptId;

    @Schema(description = "默认优先级")
    private Integer defaultPriority;

    @Schema(description = "默认 SLA 小时数")
    private Integer defaultSlaHours;

    @Schema(description = "状态：0启用 / 1禁用")
    private Integer status;

}
