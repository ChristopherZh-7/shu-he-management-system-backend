package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 外出费用记录 Response VO")
@Data
public class OutsideCostRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "外出申请ID（旧版，已废弃）")
    private Long outsideRequestId;

    @Schema(description = "服务发起ID（统一服务发起）")
    private Long serviceLaunchId;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    private String contractName;

    @Schema(description = "服务项ID")
    private Long serviceItemId;

    @Schema(description = "服务项名称")
    private String serviceItemName;

    @Schema(description = "服务类型（字典值）")
    private String serviceType;

    @Schema(description = "部门类型：1-安全服务 2-安全运营 3-数据安全")
    private Integer deptType;

    @Schema(description = "发起部门ID（A部门）")
    private Long requestDeptId;

    @Schema(description = "发起部门名称")
    private String requestDeptName;

    @Schema(description = "发起人ID")
    private Long requestUserId;

    @Schema(description = "发起人姓名")
    private String requestUserName;

    @Schema(description = "目标部门ID（B部门）")
    private Long targetDeptId;

    @Schema(description = "目标部门名称")
    private String targetDeptName;

    @Schema(description = "费用金额")
    private BigDecimal amount;

    @Schema(description = "结算人ID")
    private Long settleUserId;

    @Schema(description = "结算人姓名")
    private String settleUserName;

    @Schema(description = "结算人部门ID")
    private Long settleDeptId;

    @Schema(description = "结算人部门名称")
    private String settleDeptName;

    @Schema(description = "指派人ID（B部门负责人）")
    private Long assignUserId;

    @Schema(description = "指派人姓名")
    private String assignUserName;

    @Schema(description = "指派时间")
    private LocalDateTime assignTime;

    @Schema(description = "填写人ID")
    private Long fillUserId;

    @Schema(description = "填写人姓名")
    private String fillUserName;

    @Schema(description = "填写时间")
    private LocalDateTime fillTime;

    @Schema(description = "状态：0-待指派结算人 1-待填写金额 2-已完成")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 额外字段：外出申请相关信息
    @Schema(description = "外出地点")
    private String destination;

    @Schema(description = "外出事由")
    private String reason;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    // ========== 合同分配相关信息（用于填写费用弹窗展示） ==========

    @Schema(description = "合同总金额")
    private BigDecimal contractTotalAmount;

    @Schema(description = "合同已分配给部门的总金额")
    private BigDecimal contractAllocatedAmount;

    @Schema(description = "各部门分配列表")
    private List<DeptAllocationInfo> deptAllocations;

    @Schema(description = "该合同已产生的跨部门费用总额")
    private BigDecimal contractOutsideCostTotal;

    @Schema(description = "该合同已产生的跨部门费用笔数")
    private Integer contractOutsideCostCount;

    /**
     * 部门分配信息（简化版，用于展示）
     */
    @Data
    public static class DeptAllocationInfo {
        @Schema(description = "部门ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "分配金额")
        private BigDecimal allocatedAmount;

        @Schema(description = "是否为发起部门")
        private Boolean isRequestDept;

        @Schema(description = "是否为目标部门")
        private Boolean isTargetDept;
    }
}
