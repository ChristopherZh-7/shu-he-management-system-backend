package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 工作日信息 Response VO
 */
@Schema(description = "管理后台 - 工作日信息 Response VO")
@Data
public class WorkingDaysRespVO {

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer year;

    @Schema(description = "月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer month;

    @Schema(description = "当月总天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "31")
    private Integer totalDays;

    @Schema(description = "周末天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Integer weekendDays;

    @Schema(description = "法定节假日天数（不含周末）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer holidayDays;

    @Schema(description = "调休工作日天数（本应休息但需上班）", example = "1")
    private Integer makeupWorkDays;

    @Schema(description = "实际工作日数", requiredMode = Schema.RequiredMode.REQUIRED, example = "22")
    private Integer workingDays;

    @Schema(description = "节假日详情列表")
    private List<HolidayDetail> holidays;

    @Data
    @Schema(description = "节假日详情")
    public static class HolidayDetail {

        @Schema(description = "日期", example = "2026-01-01")
        private String date;

        @Schema(description = "节假日名称", example = "元旦")
        private String name;

        @Schema(description = "是否工作日（调休上班为true）", example = "false")
        private Boolean isWorkday;
    }

}
