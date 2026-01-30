package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 安全运营合同 Response VO")
@Data
public class SecurityOperationContractRespVO {

    @Schema(description = "主键ID")
    private Long id;

    // ========== 合同关联 ==========

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同部门分配ID")
    private Long contractDeptAllocationId;

    // ========== 客户信息 ==========

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    // ========== 驻场信息 ==========

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "驻场地点")
    private String onsiteLocation;

    @Schema(description = "详细地址")
    private String onsiteAddress;

    @Schema(description = "驻场开始日期")
    private LocalDate onsiteStartDate;

    @Schema(description = "驻场结束日期")
    private LocalDate onsiteEndDate;

    // ========== 费用 ==========

    @Schema(description = "管理费（元）")
    private BigDecimal managementFee;

    @Schema(description = "驻场费（元）")
    private BigDecimal onsiteFee;

    @Schema(description = "总费用（元）")
    private BigDecimal totalFee;

    // ========== 人员统计 ==========

    @Schema(description = "管理人员数量")
    private Integer managementCount;

    @Schema(description = "驻场人员数量")
    private Integer onsiteCount;

    // ========== 状态 ==========

    @Schema(description = "状态：0-待启动 1-进行中 2-已结束 3-已终止")
    private Integer status;

    // ========== 服务项统计 ==========

    @Schema(description = "服务项数量")
    private Integer serviceItemCount;

    // ========== 审计信息 ==========

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ========== 关联数据 ==========

    @Schema(description = "管理人员列表")
    private List<SecurityOperationMemberRespVO> managementMembers;

    @Schema(description = "驻场人员列表")
    private List<SecurityOperationMemberRespVO> onsiteMembers;

    @Schema(description = "服务项列表")
    private List<ServiceItemRespVO> serviceItems;

}
