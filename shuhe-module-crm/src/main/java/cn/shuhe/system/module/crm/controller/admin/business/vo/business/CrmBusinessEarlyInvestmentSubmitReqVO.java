package cn.shuhe.system.module.crm.controller.admin.business.vo.business;

import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 提前投入审批提交 Request VO")
@Data
public class CrmBusinessEarlyInvestmentSubmitReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商机编号不能为空")
    private Long businessId;

    @Schema(description = "投入人员列表", example = "[{\"userId\":1,\"userName\":\"张三\",\"workDays\":5}]")
    private List<CrmBusinessDO.Personnel> personnel;

    @Schema(description = "预计自垫资金（元）", example = "50000.00")
    private BigDecimal estimatedCost;

    @Schema(description = "提前投入工作内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工作内容不能为空")
    private String workScope;

    @Schema(description = "计划开始日期")
    private LocalDate planStart;

    @Schema(description = "计划结束日期")
    private LocalDate planEnd;

    @Schema(description = "若合同未签的处理方式")
    private String riskHandling;

    @Schema(description = "申请理由", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "申请理由不能为空")
    private String reason;

}
