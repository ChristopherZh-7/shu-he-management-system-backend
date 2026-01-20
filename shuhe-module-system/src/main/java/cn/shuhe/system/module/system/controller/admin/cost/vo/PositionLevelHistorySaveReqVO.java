package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 管理后台 - 职级变更记录新增/修改 Request VO
 */
@Schema(description = "管理后台 - 职级变更记录新增/修改 Request VO")
@Data
public class PositionLevelHistorySaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "变更前职级", example = "P1-1")
    private String oldPositionLevel;

    @Schema(description = "变更后职级", requiredMode = Schema.RequiredMode.REQUIRED, example = "P2-1")
    @NotNull(message = "变更后职级不能为空")
    private String newPositionLevel;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-15")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "备注", example = "年度晋升")
    private String remark;

}
