package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 项目周报/汇报创建/更新 Request VO")
@Data
public class ProjectReportSaveReqVO {

    @Schema(description = "记录ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目不能为空")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目类型不能为空")
    private Integer projectType;

    @Schema(description = "项目名称", example = "XX银行安保项目")
    private String projectName;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "周数不能为空")
    private Integer weekNumber;

    @Schema(description = "本周开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-26")
    @NotNull(message = "本周开始日期不能为空")
    private String weekStartDate;

    @Schema(description = "本周结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-30")
    @NotNull(message = "本周结束日期不能为空")
    private String weekEndDate;

    @Schema(description = "项目进展", example = "本周完成了...")
    private String progress;

    @Schema(description = "遇到的问题", example = "客户方面...")
    private String issues;

    @Schema(description = "需要的资源/协调事项", example = "需要增派人手...")
    private String resources;

    @Schema(description = "风险提示", example = "项目进度可能延迟...")
    private String risks;

    @Schema(description = "下周计划", example = "下周计划完成...")
    private String nextWeekPlan;

    @Schema(description = "附件列表")
    private List<String> attachments;

    @Schema(description = "备注")
    private String remark;

}
