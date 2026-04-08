package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 部门服务单 Response VO")
@Data
public class ProjectDeptServiceRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    // ========== 关联信息 ==========

    @Schema(description = "所属项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "项目名称", example = "某银行安全测试项目")
    private String projectName;

    @Schema(description = "项目编号", example = "PRJ-1-20260116-0001")
    private String projectCode;

    @Schema(description = "CRM合同ID", example = "1")
    private Long contractId;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "CRM客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    // ========== 部门信息 ==========

    @Schema(description = "所属部门ID", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer deptType;

    // ========== 领取信息 ==========

    @Schema(description = "领取人ID", example = "1")
    private Long claimUserId;

    @Schema(description = "领取人姓名", example = "张三")
    private String claimUserName;

    @Schema(description = "领取时间")
    private LocalDateTime claimTime;

    @Schema(description = "是否已领取", example = "true")
    private Boolean claimed;

    // ========== 负责人 ==========

    @Schema(description = "负责人ID列表", example = "[1, 2]")
    private List<Long> managerIds;

    @Schema(description = "负责人姓名列表", example = "[\"张三\", \"李四\"]")
    private List<String> managerNames;

    // ========== 安全服务专用：驻场和二线负责人 ==========

    @Schema(description = "驻场负责人ID列表（仅安全服务）", example = "[1, 2]")
    private List<Long> onsiteManagerIds;

    @Schema(description = "驻场负责人姓名列表（仅安全服务）", example = "[\"张三\", \"李四\"]")
    private List<String> onsiteManagerNames;

    @Schema(description = "二线负责人ID列表（仅安全服务）", example = "[3, 4]")
    private List<Long> secondLineManagerIds;

    @Schema(description = "二线负责人姓名列表（仅安全服务）", example = "[\"王五\", \"赵六\"]")
    private List<String> secondLineManagerNames;

    // ========== 状态和进度 ==========

    @Schema(description = "状态：0待领取 1待开始 2进行中 3已暂停 4已完成 5已取消", example = "1")
    private Integer status;

    @Schema(description = "进度百分比 0-100", example = "50")
    private Integer progress;

    // ========== 时间信息 ==========

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    // ========== 扩展字段 ==========

    @Schema(description = "描述", example = "项目描述")
    private String description;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    // ========== 统计信息 ==========

    @Schema(description = "服务项数量", example = "3")
    private Integer serviceItemCount;

    // ========== 通用字段 ==========

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}
