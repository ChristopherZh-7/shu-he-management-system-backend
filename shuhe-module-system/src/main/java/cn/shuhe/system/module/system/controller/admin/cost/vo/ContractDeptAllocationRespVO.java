package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 合同部门分配 Response VO
 */
@Schema(description = "管理后台 - 合同部门分配 Response VO")
@Data
public class ContractDeptAllocationRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "CRM合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long contractId;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    @Schema(description = "部门类型：1-安全服务 2-安全运营 3-数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "上级分配ID（NULL表示从合同直接分配的第一级）", example = "1")
    private Long parentAllocationId;

    @Schema(description = "分配层级（1=一级部门, 2=二级, 以此类推）", example = "1")
    private Integer allocationLevel;

    @Schema(description = "从上级获得的金额", example = "300000.00")
    private BigDecimal receivedAmount;

    @Schema(description = "已分配给下级的金额", example = "240000.00")
    private BigDecimal distributedAmount;

    @Schema(description = "分配金额（等于receivedAmount）", requiredMode = Schema.RequiredMode.REQUIRED, example = "240000.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "已分配给服务项的金额", example = "200000.00")
    private BigDecimal serviceItemAllocatedAmount;

    @Schema(description = "剩余可分配金额", example = "40000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "备注", example = "安全服务部份额")
    private String remark;

    @Schema(description = "服务项分配列表")
    private List<ServiceItemAllocationRespVO> serviceItemAllocations;

    @Schema(description = "子部门分配列表（树形结构）")
    private List<ContractDeptAllocationRespVO> children;

    @Schema(description = "是否可以分配给下级（当前用户是否有权限且有剩余金额）")
    private Boolean canDistribute;

    // ========== 跨部门费用相关 ==========

    @Schema(description = "跨部门费用支出（作为发起方要付给其他部门的钱）", example = "5000.00")
    private BigDecimal outsideCostExpense;

    @Schema(description = "跨部门费用支出笔数", example = "2")
    private Integer outsideCostExpenseCount;

    @Schema(description = "跨部门费用收入（作为目标方从其他部门收到的钱）", example = "3000.00")
    private BigDecimal outsideCostIncome;

    @Schema(description = "跨部门费用收入笔数", example = "1")
    private Integer outsideCostIncomeCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
