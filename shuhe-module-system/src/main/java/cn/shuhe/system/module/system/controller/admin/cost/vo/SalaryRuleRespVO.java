package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台 - 工资规则 Response VO
 */
@Schema(description = "管理后台 - 工资规则 Response VO")
@Data
public class SalaryRuleRespVO {

    @Schema(description = "部门类型工资规则列表")
    private List<DeptTypeSalaryRule> deptTypeRules;

    @Data
    @Schema(description = "部门类型工资规则")
    public static class DeptTypeSalaryRule {

        @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
        private Integer deptType;

        @Schema(description = "部门类型名称", example = "安全服务")
        private String deptTypeName;

        @Schema(description = "职级工资列表")
        private List<PositionLevelSalary> salaries;
    }

    @Data
    @Schema(description = "职级工资")
    public static class PositionLevelSalary {

        @Schema(description = "职级", example = "P1-1")
        private String positionLevel;

        @Schema(description = "基础工资", example = "6000")
        private BigDecimal salary;

        @Schema(description = "月成本", example = "10800")
        private BigDecimal monthlyCost;
    }

}
