package cn.shuhe.system.module.finance.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 财务分配 Response VO")
@Data
public class FinanceAllocationRespVO {

    @Schema(description = "主键")
    private Long id;
    @Schema(description = "父节点ID")
    private Long parentId;
    @Schema(description = "合同ID")
    private Long contractId;
    @Schema(description = "部门服务单ID")
    private Long deptServiceId;
    @Schema(description = "服务项ID")
    private Long serviceItemId;
    @Schema(description = "分配层级")
    private Integer allocationLevel;
    @Schema(description = "分配类型")
    private String allocationType;
    @Schema(description = "部门ID")
    private Long deptId;
    @Schema(description = "部门名称")
    private String deptName;
    @Schema(description = "部门类型")
    private Integer deptType;
    @Schema(description = "节点名称")
    private String name;
    @Schema(description = "分配金额")
    private BigDecimal allocatedAmount;
    @Schema(description = "子节点已分配金额")
    private BigDecimal childAllocatedAmount;
    @Schema(description = "剩余可分配金额")
    private BigDecimal remainingAmount;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "子节点")
    private List<FinanceAllocationRespVO> children;

}
