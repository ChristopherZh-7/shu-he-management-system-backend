package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 日常管理记录创建/更新 Request VO")
@Data
public class DailyManagementRecordSaveReqVO {

    @Schema(description = "记录ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "周数不能为空")
    private Integer weekNumber;

    @Schema(description = "本周开始日期（周一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-26")
    @NotNull(message = "本周开始日期不能为空")
    private String weekStartDate;

    @Schema(description = "本周结束日期（周五）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-30")
    @NotNull(message = "本周结束日期不能为空")
    private String weekEndDate;

    @Schema(description = "周一工作内容", example = "参加部门会议，讨论项目进度...")
    private String mondayContent;

    @Schema(description = "周二工作内容", example = "编写技术文档...")
    private String tuesdayContent;

    @Schema(description = "周三工作内容", example = "客户沟通会议...")
    private String wednesdayContent;

    @Schema(description = "周四工作内容", example = "代码审查...")
    private String thursdayContent;

    @Schema(description = "周五工作内容", example = "周报总结...")
    private String fridayContent;

    @Schema(description = "本周总结", example = "本周主要完成了...")
    private String weeklySummary;

    @Schema(description = "下周计划", example = "下周计划完成...")
    private String nextWeekPlan;

    @Schema(description = "附件列表")
    private List<String> attachments;

}
