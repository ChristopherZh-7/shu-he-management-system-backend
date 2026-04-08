package cn.shuhe.system.module.finance.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 合同财务概览 VO")
@Data
public class FinanceContractSummaryVO {

    @Schema(description = "合同ID")
    private Long contractId;
    @Schema(description = "合同编号")
    private String contractNo;
    @Schema(description = "合同名称")
    private String contractName;
    @Schema(description = "审核状态")
    private Integer auditStatus;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    @Schema(description = "合同总金额")
    private BigDecimal totalPrice;
    @Schema(description = "客户名称")
    private String customerName;
    @Schema(description = "负责人ID")
    private Long ownerUserId;
    @Schema(description = "负责人名称")
    private String ownerUserName;
    @Schema(description = "商机ID")
    private Long businessId;
    @Schema(description = "商机名称")
    private String businessName;
    @Schema(description = "商机总金额")
    private BigDecimal businessTotalPrice;
    @Schema(description = "参与部门列表")
    private List<DeptInfo> deptInfoList;
    @Schema(description = "已分配金额")
    private BigDecimal allocatedAmount;
    @Schema(description = "未分配金额")
    private BigDecimal unallocatedAmount;

    @Data
    public static class DeptInfo {
        @Schema(description = "部门ID")
        private Long deptId;
        @Schema(description = "部门名称")
        private String deptName;
        @Schema(description = "分配金额")
        private BigDecimal amount;
        @Schema(description = "部门负责人")
        private String leaderName;
    }

}
