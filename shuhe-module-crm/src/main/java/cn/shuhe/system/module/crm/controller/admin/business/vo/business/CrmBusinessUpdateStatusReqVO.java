package cn.shuhe.system.module.crm.controller.admin.business.vo.business;

import cn.shuhe.system.framework.common.validation.InEnum;
import cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 商机更新结束状态 Request VO")
@Data
public class CrmBusinessUpdateStatusReqVO {

    @Schema(description = "商机编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32129")
    @NotNull(message = "商机编号不能为空")
    private Long id;

    @Schema(description = "结束状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结束状态不能为空")
    @InEnum(value = CrmBusinessEndStatusEnum.class)
    private Integer endStatus;

    @Schema(description = "结束备注（退场原因等）", example = "客户预算削减，项目取消")
    private String endRemark;

}
