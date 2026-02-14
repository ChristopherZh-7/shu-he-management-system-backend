package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 项目周报/汇报分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectReportPageReqVO extends PageParam {

    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    @Schema(description = "项目类型", example = "1")
    private Integer projectType;

    @Schema(description = "年份", example = "2026")
    private Integer year;

    @Schema(description = "周数", example = "5")
    private Integer weekNumber;

    @Schema(description = "记录人ID", example = "1")
    private Long creatorId;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "关键字搜索", example = "进展")
    private String keyword;

}
