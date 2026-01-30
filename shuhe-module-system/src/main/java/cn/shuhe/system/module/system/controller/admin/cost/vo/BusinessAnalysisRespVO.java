package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析 Response VO
 */
@Schema(description = "管理后台 - 经营分析 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAnalysisRespVO {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "截止日期")
    private LocalDate cutoffDate;

    @Schema(description = "部门汇总列表")
    private List<DeptAnalysis> deptAnalysisList;

    @Schema(description = "总计")
    private TotalAnalysis total;

    // ========== 部门分析 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptAnalysis {

        @Schema(description = "部门ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全")
        private Integer deptType;

        @Schema(description = "部门类型名称")
        private String deptTypeName;

        @Schema(description = "员工人数")
        private Integer employeeCount;

        // ========== 收入 ==========

        @Schema(description = "合同收入")
        private BigDecimal contractIncome;

        @Schema(description = "跨部门收入")
        private BigDecimal outsideIncome;

        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        // ========== 支出 ==========

        @Schema(description = "员工成本")
        private BigDecimal employeeCost;

        @Schema(description = "跨部门支出")
        private BigDecimal outsideExpense;

        @Schema(description = "总支出")
        private BigDecimal totalExpense;

        // ========== 利润 ==========

        @Schema(description = "净利润")
        private BigDecimal netProfit;

        @Schema(description = "利润率(%)")
        private BigDecimal profitRate;

        // ========== 子部门/班级 ==========

        @Schema(description = "子部门列表")
        private List<SubDeptAnalysis> subDeptList;

        // ========== 直属员工 ==========

        @Schema(description = "直属员工列表（当没有子部门时显示）")
        private List<EmployeeAnalysis> employeeList;
    }

    // ========== 子部门/班级分析 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubDeptAnalysis {

        @Schema(description = "部门ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "员工人数")
        private Integer employeeCount;

        // ========== 收入 ==========

        @Schema(description = "合同收入")
        private BigDecimal contractIncome;

        @Schema(description = "跨部门收入")
        private BigDecimal outsideIncome;

        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        // ========== 支出 ==========

        @Schema(description = "员工成本")
        private BigDecimal employeeCost;

        @Schema(description = "跨部门支出")
        private BigDecimal outsideExpense;

        @Schema(description = "总支出")
        private BigDecimal totalExpense;

        // ========== 利润 ==========

        @Schema(description = "净利润")
        private BigDecimal netProfit;

        @Schema(description = "利润率(%)")
        private BigDecimal profitRate;

        // ========== 子部门（递归） ==========

        @Schema(description = "子部门列表（递归嵌套）")
        private List<SubDeptAnalysis> children;

        // ========== 员工明细 ==========

        @Schema(description = "员工列表")
        private List<EmployeeAnalysis> employeeList;
    }

    // ========== 员工分析 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeAnalysis {

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户姓名")
        private String userName;

        @Schema(description = "部门ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "职位")
        private String position;

        @Schema(description = "职级")
        private String positionLevel;

        @Schema(description = "是否管理人员（部门负责人）")
        private Boolean isLeader;

        // ========== 收入 ==========

        @Schema(description = "合同收入（参与项目/轮次获得的收入）")
        private BigDecimal contractIncome;

        @Schema(description = "跨部门收入")
        private BigDecimal outsideIncome;

        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        // ========== 支出 ==========

        @Schema(description = "员工成本")
        private BigDecimal employeeCost;

        @Schema(description = "跨部门支出")
        private BigDecimal outsideExpense;

        @Schema(description = "总支出")
        private BigDecimal totalExpense;

        // ========== 利润 ==========

        @Schema(description = "净利润")
        private BigDecimal netProfit;

        @Schema(description = "利润率(%)")
        private BigDecimal profitRate;

        // ========== 参与项目/轮次明细 ==========

        @Schema(description = "参与的项目/合同明细")
        private List<ProjectParticipation> participationList;
    }

    // ========== 项目参与明细 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectParticipation {

        @Schema(description = "项目/合同ID")
        private Long projectId;

        @Schema(description = "项目/合同名称")
        private String projectName;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "参与类型：management-管理 onsite-驻场 executor-执行")
        private String participationType;

        @Schema(description = "参与类型名称")
        private String participationTypeName;

        @Schema(description = "开始日期")
        private LocalDate startDate;

        @Schema(description = "结束日期")
        private LocalDate endDate;

        @Schema(description = "工作日数/完成轮次数")
        private Integer workDays;

        @Schema(description = "获得收入")
        private BigDecimal income;
    }

    // ========== 总计 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalAnalysis {

        @Schema(description = "总员工数")
        private Integer employeeCount;

        @Schema(description = "总合同收入")
        private BigDecimal contractIncome;

        @Schema(description = "总跨部门收入")
        private BigDecimal outsideIncome;

        @Schema(description = "总收入")
        private BigDecimal totalIncome;

        @Schema(description = "总员工成本")
        private BigDecimal employeeCost;

        @Schema(description = "总跨部门支出")
        private BigDecimal outsideExpense;

        @Schema(description = "总支出")
        private BigDecimal totalExpense;

        @Schema(description = "总净利润")
        private BigDecimal netProfit;

        @Schema(description = "总利润率(%)")
        private BigDecimal profitRate;
    }

}
