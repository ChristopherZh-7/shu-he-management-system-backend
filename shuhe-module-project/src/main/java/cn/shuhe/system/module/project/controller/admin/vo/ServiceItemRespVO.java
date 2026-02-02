package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 服务项 Response VO")
@Data
public class ServiceItemRespVO {

    @Schema(description = "服务项ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    @Schema(description = "服务项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRJ-1-20260116-0001")
    private String code;

    @Schema(description = "服务项名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某银行渗透测试")
    private String name;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer deptType;

    @Schema(description = "服务模式：1驻场 2二线", example = "2")
    private Integer serviceMode;

    @Schema(description = "服务类型", example = "penetration_test")
    private String serviceType;

    @Schema(description = "服务项描述", example = "对某银行系统进行渗透测试")
    private String description;

    // ========== 客户信息 ==========

    @Schema(description = "CRM客户ID", example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "CRM合同ID", example = "1")
    private Long contractId;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "合同名称", example = "2026年安全服务项目")
    private String contractName;

    // ========== 时间信息 ==========

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    // ========== 人员信息 ==========

    @Schema(description = "所属部门ID", example = "100")
    private Long deptId;

    // ========== 状态进度 ==========

    @Schema(description = "服务项状态：0草稿 1进行中 2已暂停 3已完成 4已取消", example = "1")
    private Integer status;

    @Schema(description = "进度百分比 0-100", example = "50")
    private Integer progress;

    @Schema(description = "优先级：0低 1中 2高", example = "1")
    private Integer priority;

    // ========== 服务频次配置 ==========

    @Schema(description = "频次类型：0按需(不限) 1按月 2按季 3按年 4按周", example = "1")
    private Integer frequencyType;

    @Schema(description = "每周期最大执行次数（按需时无效）", example = "2")
    private Integer maxCount;

    @Schema(description = "历史执行总次数", example = "5")
    private Integer usedCount;

    // ========== 扩展字段 ==========

    @Schema(description = "标签列表", example = "[\"渗透\", \"银行\"]")
    private List<String> tags;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    // ========== 通用字段 ==========

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}
