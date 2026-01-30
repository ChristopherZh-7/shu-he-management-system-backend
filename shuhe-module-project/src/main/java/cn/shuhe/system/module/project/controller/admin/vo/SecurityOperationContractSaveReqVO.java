package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 安全运营合同新增/修改 Request VO")
@Data
public class SecurityOperationContractSaveReqVO {

    @Schema(description = "主键ID，更新时必填")
    private Long id;

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同不能为空")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

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

    @Schema(description = "管理费（元）")
    private BigDecimal managementFee;

    @Schema(description = "驻场费（元）")
    private BigDecimal onsiteFee;

    @Schema(description = "管理人员列表")
    private List<SecurityOperationMemberSaveReqVO> managementMembers;

    @Schema(description = "驻场人员列表")
    private List<SecurityOperationMemberSaveReqVO> onsiteMembers;

}
