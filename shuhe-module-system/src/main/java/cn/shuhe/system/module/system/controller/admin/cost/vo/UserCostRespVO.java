package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 用户成本 Response VO
 */
@Schema(description = "管理后台 - 用户成本 Response VO")
@Data
public class UserCostRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "用户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String nickname;

    @Schema(description = "用户账号", example = "zhangsan")
    private String username;

    @Schema(description = "部门编号", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "部门类型名称", example = "安全服务")
    private String deptTypeName;

    @Schema(description = "职级", example = "P2-1")
    private String positionLevel;

    @Schema(description = "入职日期")
    private LocalDateTime hireDate;

    @Schema(description = "基础工资", requiredMode = Schema.RequiredMode.REQUIRED, example = "9000")
    private BigDecimal baseSalary;

    @Schema(description = "月成本（工资*1.3+3000）", requiredMode = Schema.RequiredMode.REQUIRED, example = "14700")
    private BigDecimal monthlyCost;

    @Schema(description = "日成本", requiredMode = Schema.RequiredMode.REQUIRED, example = "668.18")
    private BigDecimal dailyCost;

    @Schema(description = "当月工作日数", example = "22")
    private Integer workingDays;

    @Schema(description = "计算年份", example = "2026")
    private Integer year;

    @Schema(description = "计算月份", example = "1")
    private Integer month;

    // ========== 年度累计成本字段 ==========

    @Schema(description = "本年度已工作天数（从入职日或年初算起）", example = "15")
    private Integer yearToDateWorkingDays;

    @Schema(description = "本年度累计成本", example = "10022.70")
    private BigDecimal yearToDateCost;

    @Schema(description = "成本计算起始日期（入职日或年初）", example = "2026-01-01")
    private String costStartDate;

    @Schema(description = "成本计算截止日期（今天或年末）", example = "2026-01-19")
    private String costEndDate;

}
