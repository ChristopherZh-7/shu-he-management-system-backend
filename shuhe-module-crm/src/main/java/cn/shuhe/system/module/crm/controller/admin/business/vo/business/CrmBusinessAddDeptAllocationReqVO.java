package cn.shuhe.system.module.crm.controller.admin.business.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - CRM 商机补充部门分配 Request VO")
@Data
public class CrmBusinessAddDeptAllocationReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "商机编号不能为空")
    private Long businessId;

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "119")
    @NotNull(message = "部门编号不能为空")
    private Long deptId;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "500000")
    @NotNull(message = "分配金额不能为空")
    private BigDecimal amount;

}
