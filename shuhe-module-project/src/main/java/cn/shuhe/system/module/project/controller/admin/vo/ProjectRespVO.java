package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 项目 Response VO")
@Data
public class ProjectRespVO {

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某银行安全测试项目")
    private String name;

    @Schema(description = "项目编号", example = "PRJ-1-20260116-0001")
    private String code;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
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

    // ========== 项目负责人 ==========

    @Schema(description = "项目负责人ID列表", example = "[1, 2]")
    private List<Long> managerIds;

    @Schema(description = "项目负责人姓名列表", example = "[\"张三\", \"李四\"]")
    private List<String> managerNames;

    // ========== 状态 ==========

    @Schema(description = "项目状态：0草稿 1进行中 2已完成", example = "1")
    private Integer status;

    @Schema(description = "项目描述", example = "某银行安全测试项目描述")
    private String description;

    // ========== 统计信息 ==========

    @Schema(description = "服务项数量", example = "3")
    private Integer serviceItemCount;

    // ========== 通用字段 ==========

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}
