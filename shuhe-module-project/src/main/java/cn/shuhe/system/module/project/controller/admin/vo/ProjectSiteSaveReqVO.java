package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理后台 - 项目驻场点新增/修改 Request VO
 */
@Schema(description = "管理后台 - 项目驻场点新增/修改 Request VO")
@Data
public class ProjectSiteSaveReqVO {

    @Schema(description = "驻场点ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "驻场点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户总部")
    @NotBlank(message = "驻场点名称不能为空")
    private String name;

    @Schema(description = "详细地址", example = "上海市浦东新区XX路XX号")
    private String address;

    @Schema(description = "联系人姓名", example = "张经理")
    private String contactName;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "服务要求", example = "24小时值班、门禁管理")
    private String serviceRequirement;

    @Schema(description = "人员配置（需要驻场人数）", example = "3")
    private Integer staffCount;

    @Schema(description = "开始日期", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "备注", example = "需持有安全员证书")
    private String remark;

}
