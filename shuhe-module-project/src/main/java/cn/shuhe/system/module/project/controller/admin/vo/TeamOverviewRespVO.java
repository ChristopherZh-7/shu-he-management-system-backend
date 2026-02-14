package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 团队工作总览 Response VO")
@Data
public class TeamOverviewRespVO {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "周数")
    private Integer weekNumber;

    @Schema(description = "周开始日期")
    private LocalDate weekStartDate;

    @Schema(description = "周结束日期")
    private LocalDate weekEndDate;

    @Schema(description = "团队成员工作汇总")
    private List<MemberWorkSummary> memberSummaries;

    /**
     * 每个成员的工作汇总
     */
    @Data
    public static class MemberWorkSummary {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "工作模式 1-驻场(在岗) 2-二线 3-未入场")
        private Integer workMode;

        @Schema(description = "是否管理人员（部门负责人）")
        private Boolean isManager;

        @Schema(description = "项目工作记录数量")
        private Integer projectRecordCount;

        @Schema(description = "日常记录填写天数")
        private Integer dailyContentDays;

        @Schema(description = "是否有周总结")
        private Boolean hasWeeklySummary;

        @Schema(description = "周总结内容")
        private String weeklySummary;

        @Schema(description = "下周计划")
        private String nextWeekPlan;

        @Schema(description = "每日详情")
        private List<DayDetail> dayDetails;

    }

    /**
     * 每日详情
     */
    @Data
    public static class DayDetail {

        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "星期名称")
        private String dayOfWeekName;

        @Schema(description = "日常管理内容")
        private String dailyContent;

        @Schema(description = "项目记录列表")
        private List<ProjectWorkRecordRespVO> projectRecords;

        @Schema(description = "项目记录数量")
        private Integer projectRecordCount;

    }

}
