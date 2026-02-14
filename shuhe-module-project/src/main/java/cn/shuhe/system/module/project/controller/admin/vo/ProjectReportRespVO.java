package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 项目周报/汇报 Response VO")
@Data
public class ProjectReportRespVO {

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全", example = "1")
    private Integer projectType;

    @Schema(description = "项目名称", example = "XX银行安保项目")
    private String projectName;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer weekNumber;

    @Schema(description = "本周开始日期", example = "2026-01-26")
    private LocalDate weekStartDate;

    @Schema(description = "本周结束日期", example = "2026-01-30")
    private LocalDate weekEndDate;

    @Schema(description = "项目进展")
    private String progress;

    @Schema(description = "遇到的问题")
    private String issues;

    @Schema(description = "需要的资源/协调事项")
    private String resources;

    @Schema(description = "风险提示")
    private String risks;

    @Schema(description = "下周计划")
    private String nextWeekPlan;

    @Schema(description = "附件列表")
    private List<String> attachments;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "记录人ID", example = "1")
    private String creator;

    @Schema(description = "记录人姓名", example = "张三")
    private String creatorName;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全运营服务部")
    private String deptName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
