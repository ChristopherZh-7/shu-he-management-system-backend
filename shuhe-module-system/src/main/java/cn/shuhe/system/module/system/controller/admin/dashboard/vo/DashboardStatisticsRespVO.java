package cn.shuhe.system.module.system.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表板统计数据响应 VO
 */
@Schema(description = "管理后台 - 仪表板统计数据 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsRespVO {

    // ========== 统计卡片数据 ==========

    @Schema(description = "项目统计")
    private ProjectStats projectStats;

    @Schema(description = "合同统计")
    private ContractStats contractStats;

    @Schema(description = "客户统计")
    private CustomerStats customerStats;

    @Schema(description = "任务统计")
    private TaskStats taskStats;

    @Schema(description = "收入统计")
    private RevenueStats revenueStats;

    // ========== 图表数据 ==========

    @Schema(description = "经营趋势（月度）")
    private List<TrendData> trendData;

    @Schema(description = "项目状态分布")
    private List<PieChartData> projectDistribution;

    @Schema(description = "任务状态分布")
    private List<PieChartData> taskDistribution;

    @Schema(description = "部门排行")
    private List<RankData> deptRanking;

    // ========== 列表数据 ==========

    @Schema(description = "待办任务列表")
    private List<TodoItem> todoList;

    @Schema(description = "最近活动")
    private List<ActivityItem> recentActivities;

    @Schema(description = "合同提醒")
    private List<ContractRemindItem> contractReminders;

    // ========== 内部类定义 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStats {
        @Schema(description = "进行中项目数")
        private Integer activeCount;
        @Schema(description = "项目总数")
        private Integer totalCount;
        @Schema(description = "本月新增")
        private Integer monthlyNewCount;
        @Schema(description = "已完成项目数")
        private Integer completedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractStats {
        @Schema(description = "进行中合同数")
        private Integer activeCount;
        @Schema(description = "合同总数")
        private Integer totalCount;
        @Schema(description = "待审核数")
        private Integer pendingAuditCount;
        @Schema(description = "即将到期数")
        private Integer expiringCount;
        @Schema(description = "合同总金额（元）")
        private BigDecimal totalAmount;
        @Schema(description = "本月合同金额（元）")
        private BigDecimal monthlyAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerStats {
        @Schema(description = "客户总数")
        private Integer totalCount;
        @Schema(description = "今日需联系数")
        private Integer todayContactCount;
        @Schema(description = "待跟进数")
        private Integer followUpCount;
        @Schema(description = "本月新增")
        private Integer monthlyNewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStats {
        @Schema(description = "待办任务数")
        private Integer todoCount;
        @Schema(description = "今日完成数")
        private Integer todayDoneCount;
        @Schema(description = "本周完成数")
        private Integer weeklyDoneCount;
        @Schema(description = "逾期任务数")
        private Integer overdueCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        @Schema(description = "本月收入")
        private BigDecimal monthlyRevenue;
        @Schema(description = "本月成本")
        private BigDecimal monthlyCost;
        @Schema(description = "本月利润")
        private BigDecimal monthlyProfit;
        @Schema(description = "同比增长率")
        private BigDecimal growthRate;
        @Schema(description = "年度累计收入")
        private BigDecimal yearlyRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceivableStats {
        @Schema(description = "待回款数量")
        private Integer pendingCount;
        @Schema(description = "待回款总金额（元）")
        private BigDecimal pendingAmount;
        @Schema(description = "已逾期数量")
        private Integer overdueCount;
        @Schema(description = "本月已回款金额（元）")
        private BigDecimal monthlyReceivedAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        @Schema(description = "月份")
        private String month;
        @Schema(description = "收入")
        private BigDecimal revenue;
        @Schema(description = "成本")
        private BigDecimal cost;
        @Schema(description = "利润")
        private BigDecimal profit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieChartData {
        @Schema(description = "名称")
        private String name;
        @Schema(description = "值")
        private Integer value;
        @Schema(description = "颜色（可选）")
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankData {
        @Schema(description = "排名")
        private Integer rank;
        @Schema(description = "部门名称")
        private String deptName;
        @Schema(description = "业绩金额")
        private BigDecimal amount;
        @Schema(description = "完成率")
        private BigDecimal completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItem {
        @Schema(description = "任务ID")
        private String id;
        @Schema(description = "任务标题")
        private String title;
        @Schema(description = "流程名称")
        private String processName;
        @Schema(description = "创建时间描述")
        private String createTimeDesc;
        @Schema(description = "状态: pending, urgent")
        private String status;
        @Schema(description = "流程实例ID")
        private String processInstanceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        @Schema(description = "活动ID")
        private Long id;
        @Schema(description = "活动类型: project, contract, customer, task")
        private String type;
        @Schema(description = "活动标题")
        private String title;
        @Schema(description = "活动描述")
        private String description;
        @Schema(description = "操作人")
        private String operator;
        @Schema(description = "时间描述")
        private String timeDesc;
        @Schema(description = "关联ID")
        private Long refId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractRemindItem {
        @Schema(description = "合同ID")
        private Long id;
        @Schema(description = "合同编号")
        private String contractNo;
        @Schema(description = "合同名称")
        private String contractName;
        @Schema(description = "客户名称")
        private String customerName;
        @Schema(description = "到期日期")
        private String endDate;
        @Schema(description = "剩余天数")
        private Integer remainingDays;
        @Schema(description = "合同金额")
        private BigDecimal amount;
    }

}
