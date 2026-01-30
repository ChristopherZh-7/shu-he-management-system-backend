package cn.shuhe.system.module.system.controller.admin.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表板配置 Response VO
 */
@Schema(description = "管理后台 - 仪表板配置 Response VO")
@Data
public class DashboardConfigRespVO {

    @Schema(description = "配置ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "页面类型", example = "analytics")
    private String pageType;

    @Schema(description = "布局配置JSON", example = "[{\"i\":\"widget-1\",\"x\":0,\"y\":0,\"w\":3,\"h\":2}]")
    private String layoutConfig;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
