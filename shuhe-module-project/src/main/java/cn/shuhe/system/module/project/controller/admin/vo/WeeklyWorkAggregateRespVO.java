package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 周工作聚合响应 VO
 * 聚合日常管理记录和项目管理记录，按日期展示
 */
@Schema(description = "管理后台 - 周工作聚合 Response VO")
@Data
public class WeeklyWorkAggregateRespVO {

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer weekNumber;

    @Schema(description = "本周开始日期（周一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-26")
    private LocalDate weekStartDate;

    @Schema(description = "本周结束日期（周五）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-30")
    private LocalDate weekEndDate;

    @Schema(description = "每日工作列表（周一到周五）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DailyWorkVO> dailyWorks;

    @Schema(description = "本周总结（来自日常管理记录）")
    private String weeklySummary;

    @Schema(description = "下周计划（来自日常管理记录）")
    private String nextWeekPlan;

    @Schema(description = "日常管理记录ID（如果存在）")
    private Long dailyRecordId;

    @Schema(description = "日常管理记录附件")
    private List<String> dailyRecordAttachments;

    /**
     * 每日工作详情
     */
    @Schema(description = "每日工作详情")
    @Data
    public static class DailyWorkVO {

        @Schema(description = "日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-27")
        private LocalDate date;

        @Schema(description = "星期几（1=周一，5=周五）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer dayOfWeek;

        @Schema(description = "星期名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "周一")
        private String dayOfWeekName;

        @Schema(description = "日常管理内容（来自日常管理记录表的当天内容）")
        private String dailyContent;

        @Schema(description = "项目管理记录列表（当天的所有项目记录）")
        private List<ProjectWorkRecordRespVO> projectRecords;

        @Schema(description = "当天项目记录数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        private Integer projectRecordCount;

        @Schema(description = "是否为今天", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean isToday;
    }

}
