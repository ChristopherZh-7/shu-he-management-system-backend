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
 * 部门费用汇总 响应 VO
 */
@Schema(description = "管理后台 - 部门费用汇总 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptCostSummaryRespVO {

    @Schema(description = "统计年份", example = "2026")
    private Integer year;

    @Schema(description = "截止日期", example = "2026-01-28")
    private LocalDate cutoffDate;

    @Schema(description = "部门汇总列表")
    private List<DeptSummary> deptSummaries;

    @Schema(description = "总计")
    private TotalSummary total;

    /**
     * 单个部门的汇总数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptSummary {

        @Schema(description = "部门ID", example = "1")
        private Long deptId;

        @Schema(description = "部门名称", example = "安全运营部")
        private String deptName;

        @Schema(description = "部门类型：1-安全服务 2-安全运营 3-数据安全", example = "2")
        private Integer deptType;

        @Schema(description = "部门类型名称", example = "安全运营")
        private String deptTypeName;

        // ========== 收入部分 ==========

        @Schema(description = "合同收入（按工作日比例计算的已确认收入）", example = "462000.00")
        private BigDecimal contractIncome;

        @Schema(description = "合同数量", example = "5")
        private Integer contractCount;

        @Schema(description = "跨部门收入（被其他部门借人）", example = "30000.00")
        private BigDecimal outsideIncome;

        @Schema(description = "跨部门收入笔数", example = "3")
        private Integer outsideIncomeCount;

        // ========== 支出部分 ==========

        @Schema(description = "员工成本（年度累计）", example = "280000.00")
        private BigDecimal employeeCost;

        @Schema(description = "员工人数", example = "10")
        private Integer employeeCount;

        @Schema(description = "跨部门支出（借其他部门的人）", example = "50000.00")
        private BigDecimal outsideExpense;

        @Schema(description = "跨部门支出笔数", example = "2")
        private Integer outsideExpenseCount;

        // ========== 汇总 ==========

        @Schema(description = "总收入 = 合同收入 + 跨部门收入", example = "492000.00")
        private BigDecimal totalIncome;

        @Schema(description = "总支出 = 员工成本 + 跨部门支出", example = "330000.00")
        private BigDecimal totalExpense;

        @Schema(description = "净利润 = 总收入 - 总支出", example = "162000.00")
        private BigDecimal netProfit;

        @Schema(description = "利润率 = 净利润 / 总收入 * 100", example = "32.93")
        private BigDecimal profitRate;

        // ========== 合同明细（可选） ==========

        @Schema(description = "合同收入明细列表")
        private List<ContractIncomeDetail> contractDetails;

    }

    /**
     * 单个合同的收入明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractIncomeDetail {

        @Schema(description = "合同ID", example = "1")
        private Long contractId;

        @Schema(description = "合同编号", example = "HT-2025-001")
        private String contractNo;

        @Schema(description = "客户名称", example = "ABC公司")
        private String customerName;

        @Schema(description = "合同分配金额", example = "700000.00")
        private BigDecimal allocatedAmount;

        @Schema(description = "合同开始日期", example = "2025-06-01")
        private LocalDate startDate;

        @Schema(description = "合同结束日期", example = "2026-06-01")
        private LocalDate endDate;

        @Schema(description = "合同总工作日", example = "250")
        private Integer totalWorkDays;

        @Schema(description = "已执行工作日", example = "165")
        private Integer executedWorkDays;

        @Schema(description = "执行进度百分比", example = "66.00")
        private BigDecimal progressRate;

        @Schema(description = "已确认收入", example = "462000.00")
        private BigDecimal confirmedIncome;

        @Schema(description = "跨部门支出", example = "50000.00")
        private BigDecimal outsideExpense;

        @Schema(description = "跨部门收入", example = "30000.00")
        private BigDecimal outsideIncome;

        @Schema(description = "净收入 = 已确认收入 - 跨部门支出 + 跨部门收入", example = "442000.00")
        private BigDecimal netIncome;

    }

    /**
     * 所有部门的总计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalSummary {

        @Schema(description = "合同收入总计", example = "1000000.00")
        private BigDecimal contractIncome;

        @Schema(description = "合同数量总计", example = "15")
        private Integer contractCount;

        @Schema(description = "跨部门收入总计", example = "60000.00")
        private BigDecimal outsideIncome;

        @Schema(description = "员工成本总计", example = "650000.00")
        private BigDecimal employeeCost;

        @Schema(description = "员工人数总计", example = "30")
        private Integer employeeCount;

        @Schema(description = "跨部门支出总计", example = "55000.00")
        private BigDecimal outsideExpense;

        @Schema(description = "总收入", example = "1060000.00")
        private BigDecimal totalIncome;

        @Schema(description = "总支出", example = "705000.00")
        private BigDecimal totalExpense;

        @Schema(description = "净利润总计", example = "355000.00")
        private BigDecimal netProfit;

        @Schema(description = "整体利润率", example = "33.49")
        private BigDecimal profitRate;

    }

}
