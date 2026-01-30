package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 部门费用汇总 请求 VO
 */
@Schema(description = "管理后台 - 部门费用汇总 Request VO")
@Data
public class DeptCostSummaryReqVO {

    @Schema(description = "截止日期，默认为今天", example = "2026-01-28")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate cutoffDate;

    @Schema(description = "年份，默认为当前年", example = "2026")
    private Integer year;

    @Schema(description = "部门ID，不传则查询所有三个部门", example = "1")
    private Long deptId;

}
