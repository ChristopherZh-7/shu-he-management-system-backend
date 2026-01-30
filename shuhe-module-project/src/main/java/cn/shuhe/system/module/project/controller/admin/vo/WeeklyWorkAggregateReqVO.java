package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 周工作聚合查询请求 VO
 */
@Schema(description = "管理后台 - 周工作聚合查询 Request VO")
@Data
public class WeeklyWorkAggregateReqVO {

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年份不能为空")
    @Min(value = 2020, message = "年份不能小于2020")
    @Max(value = 2099, message = "年份不能大于2099")
    private Integer year;

    @Schema(description = "周数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "周数不能为空")
    @Min(value = 1, message = "周数不能小于1")
    @Max(value = 53, message = "周数不能大于53")
    private Integer weekNumber;

    @Schema(description = "用户ID（可选，默认当前用户）", example = "1")
    private Long userId;

    @Schema(description = "是否包含项目记录详情", example = "true")
    private Boolean includeProjectRecords = true;

}
