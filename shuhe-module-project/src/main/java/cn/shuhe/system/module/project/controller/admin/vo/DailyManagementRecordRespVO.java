package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 日常管理记录 Response VO")
@Data
public class DailyManagementRecordRespVO {

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer weekNumber;

    @Schema(description = "本周开始日期（周一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-26")
    private LocalDate weekStartDate;

    @Schema(description = "本周结束日期（周五）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-30")
    private LocalDate weekEndDate;

    @Schema(description = "周一工作内容")
    private String mondayContent;

    @Schema(description = "周二工作内容")
    private String tuesdayContent;

    @Schema(description = "周三工作内容")
    private String wednesdayContent;

    @Schema(description = "周四工作内容")
    private String thursdayContent;

    @Schema(description = "周五工作内容")
    private String fridayContent;

    @Schema(description = "本周总结")
    private String weeklySummary;

    @Schema(description = "下周计划")
    private String nextWeekPlan;

    @Schema(description = "附件列表")
    private List<String> attachments;

    @Schema(description = "记录人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String creator;

    @Schema(description = "记录人姓名", example = "张三")
    private String creatorName;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全运营服务部")
    private String deptName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}
