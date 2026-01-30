package cn.shuhe.system.module.system.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 仪表板配置 保存 Request VO
 */
@Schema(description = "管理后台 - 仪表板配置保存 Request VO")
@Data
public class DashboardConfigSaveReqVO {

    @Schema(description = "页面类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "analytics")
    @NotBlank(message = "页面类型不能为空")
    private String pageType;

    @Schema(description = "布局配置JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "布局配置不能为空")
    private String layoutConfig;

}
