package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 全局总览 Response VO")
@Data
public class GlobalOverviewRespVO {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "周数")
    private Integer weekNumber;

    @Schema(description = "周开始日期")
    private LocalDate weekStartDate;

    @Schema(description = "周结束日期")
    private LocalDate weekEndDate;

    // ===== 人员统计 =====

    @Schema(description = "总人数")
    private Integer totalStaff;

    @Schema(description = "驻场在岗人数")
    private Integer onSiteCount;

    @Schema(description = "未入场人数（驻场部门但不在项目上）")
    private Integer pendingCount;

    @Schema(description = "二线人数")
    private Integer backOfficeCount;

    // ===== 记录填写统计 =====

    @Schema(description = "本周已填写记录的人数")
    private Integer filledCount;

    @Schema(description = "本周未填写记录的人数")
    private Integer unfilledCount;

    // ===== 按部门统计 =====

    @Schema(description = "各部门工作统计")
    private List<DeptStat> deptStats;

    // ===== 按项目统计 =====

    @Schema(description = "各项目本周工作统计")
    private List<ProjectStat> projectStats;

    // ===== 主管周报 =====

    @Schema(description = "各主管的周报/管理总结")
    private List<ManagerReport> managerReports;

    // ===== 未填写记录的人员名单 =====

    @Schema(description = "未填写工作记录的人员")
    private List<UnfilledMember> unfilledMembers;

    @Data
    public static class DeptStat {

        @Schema(description = "部门ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "部门总人数")
        private Integer totalMembers;

        @Schema(description = "驻场在岗人数")
        private Integer onSiteCount;

        @Schema(description = "本周项目记录数")
        private Integer projectRecordCount;

        @Schema(description = "已填写记录人数")
        private Integer filledCount;

        @Schema(description = "未填写记录人数")
        private Integer unfilledCount;
    }

    @Data
    public static class ProjectStat {

        @Schema(description = "项目ID")
        private Long projectId;

        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "本周参与人数")
        private Integer memberCount;

        @Schema(description = "本周工作记录数")
        private Integer recordCount;

        @Schema(description = "参与人员名单")
        private List<String> memberNames;
    }

    @Data
    public static class ManagerReport {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "本周管理总结")
        private String weeklySummary;

        @Schema(description = "下周计划")
        private String nextWeekPlan;

        @Schema(description = "是否已填写")
        private Boolean hasFilled;

        @Schema(description = "项目反馈列表")
        private List<ProjectReportRespVO> projectReports;
    }

    @Data
    public static class UnfilledMember {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户昵称")
        private String nickname;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "工作模式 1-驻场 2-二线 3-未入场")
        private Integer workMode;
    }

}
