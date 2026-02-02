package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 管理后台 - 项目驻场人员新增/修改 Request VO
 */
@Schema(description = "管理后台 - 项目驻场人员新增/修改 Request VO")
@Data
public class ProjectSiteMemberSaveReqVO {

    @Schema(description = "主键ID，更新时必填")
    private Long id;

    @Schema(description = "驻场点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "驻场点ID不能为空")
    private Long siteId;

    @Schema(description = "用户ID（创建时必填）")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "人员类型：1-管理人员 2-驻场人员", example = "2")
    private Integer memberType;

    @Schema(description = "是否项目负责人：0-否 1-是", example = "0")
    private Integer isLeader;

    @Schema(description = "岗位代码")
    private String positionCode;

    @Schema(description = "岗位名称", example = "安全工程师")
    private String positionName;

    @Schema(description = "入场日期", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "离开日期", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "状态：0-待入场 1-在岗 2-已离开", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
