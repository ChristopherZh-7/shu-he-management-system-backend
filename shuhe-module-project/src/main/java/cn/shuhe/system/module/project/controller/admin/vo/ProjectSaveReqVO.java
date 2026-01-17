package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 项目保存 Request VO")
@Data
public class ProjectSaveReqVO {

    @Schema(description = "项目ID，新增时不传", example = "1")
    private Long id;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某银行安全测试项目")
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "部门类型不能为空")
    private Integer deptType;

    // ========== 客户信息 ==========

    @Schema(description = "CRM客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "CRM合同ID", example = "1")
    private Long contractId;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    // ========== 状态 ==========

    @Schema(description = "项目状态：0草稿 1进行中 2已完成", example = "0")
    private Integer status;

    @Schema(description = "项目描述", example = "某银行安全测试项目描述")
    private String description;

}
