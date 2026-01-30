package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 经营分析 Request VO
 */
@Schema(description = "管理后台 - 经营分析 Request VO")
@Data
public class BusinessAnalysisReqVO {

    @Schema(description = "截止日期，默认为今天", example = "2026-01-28")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate cutoffDate;

    @Schema(description = "年份，默认为当前年", example = "2026")
    private Integer year;

    @Schema(description = "部门ID，不传则根据权限自动判断")
    private Long deptId;

    @Schema(description = "查询级别：1-部门汇总 2-子部门/班级 3-员工明细", example = "1")
    private Integer level;

    @Schema(description = "是否包含员工明细", example = "false")
    private Boolean includeEmployees;

}
