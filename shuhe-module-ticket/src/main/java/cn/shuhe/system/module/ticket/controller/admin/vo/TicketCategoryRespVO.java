package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 工单分类 Response VO（支持树形）")
@Data
public class TicketCategoryRespVO {

    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "父分类 ID")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类编码")
    private String code;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子分类（树形展开）")
    private List<TicketCategoryRespVO> children = new ArrayList<>();

}
