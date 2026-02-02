package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台 - 项目驻场人员 Response VO
 */
@Schema(description = "管理后台 - 项目驻场人员 Response VO")
@Data
public class ProjectSiteMemberRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "驻场点ID")
    private Long siteId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "人员类型：1-管理人员 2-驻场人员")
    private Integer memberType;

    @Schema(description = "人员类型名称")
    private String memberTypeName;

    @Schema(description = "是否项目负责人：0-否 1-是")
    private Integer isLeader;

    @Schema(description = "岗位代码")
    private String positionCode;

    @Schema(description = "岗位名称")
    private String positionName;

    @Schema(description = "入场日期")
    private LocalDate startDate;

    @Schema(description = "离开日期")
    private LocalDate endDate;

    @Schema(description = "状态：0-待入场 1-在岗 2-已离开")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
