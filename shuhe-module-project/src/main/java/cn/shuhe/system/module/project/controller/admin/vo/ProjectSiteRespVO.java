package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 项目驻场点 Response VO
 */
@Schema(description = "管理后台 - 项目驻场点 Response VO")
@Data
public class ProjectSiteRespVO {

    @Schema(description = "驻场点ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "部门类型：1-安全服务 2-安全运营 3-数据安全", example = "2")
    private Integer deptType;

    @Schema(description = "驻场点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "客户总部")
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

    @Schema(description = "结束日期", example = "2027-01-31")
    private LocalDate endDate;

    @Schema(description = "状态：0-停用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "需持有安全员证书")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ========== 关联数据 ==========

    @Schema(description = "驻场人员列表")
    private List<ProjectSiteMemberRespVO> members;

    @Schema(description = "当前在岗人员数量", example = "2")
    private Integer memberCount;

    @Schema(description = "计划驻场人数", example = "3")
    private Integer plannedMemberCount;

    @Schema(description = "实际在场的驻场人员数量（不包含管理人员）", example = "2")
    private Integer activeOnsiteMemberCount;

    @Schema(description = "管理人员数量（单独展示，不计入实际驻场人数）", example = "1")
    private Integer managementMemberCount;

    @Schema(description = "驻场缺口人数", example = "1")
    private Integer staffingGap;

    @Schema(description = "是否已有有效驻场服务项")
    private Boolean hasOnsiteServiceItem;

    @Schema(description = "交付状态：0计划中 1待入场 2驻场中 3已退场")
    private Integer deliveryStatus;

    @Schema(description = "交付状态名称")
    private String deliveryStatusName;

    @Schema(description = "日期来源：signed_contract / approved_early_investment / manual")
    private String dateSource;

    @Schema(description = "日期来源名称")
    private String dateSourceName;

}
