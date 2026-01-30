package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 安全运营人员新增/修改 Request VO")
@Data
public class SecurityOperationMemberSaveReqVO {

    @Schema(description = "主键ID，更新时必填")
    private Long id;

    @Schema(description = "安全运营合同ID")
    private Long soContractId;

    @Schema(description = "驻场点ID")
    private Long siteId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "人员类型：1-管理人员 2-驻场人员")
    private Integer memberType;

    @Schema(description = "是否项目负责人：0-否 1-是")
    private Integer isLeader;

    @Schema(description = "岗位代码")
    private String positionCode;

    @Schema(description = "岗位名称")
    private String positionName;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "状态：0-待入场 1-在岗 2-已离开")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
