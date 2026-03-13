package cn.shuhe.system.module.crm.controller.admin.product.vo.product;

import cn.shuhe.system.module.crm.framework.operatelog.core.CrmProductStatusParseFunction;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 服务项创建/修改 Request VO（简化版：仅名称、状态、描述、负责人）")
@Data
public class CrmProductSaveReqVO {

    @Schema(description = "服务项编号", example = "20529")
    private Long id;

    @Schema(description = "服务项名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "渗透测试")
    @NotNull(message = "服务项名称不能为空")
    @DiffLogField(name = "服务项名称")
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "开启")
    @NotNull(message = "状态不能为空")
    @DiffLogField(name = "状态", function = CrmProductStatusParseFunction.NAME)
    private Integer status;

    @Schema(description = "服务项描述", example = "安全服务描述")
    @DiffLogField(name = "服务项描述")
    private String description;

    @Schema(description = "负责人的用户编号", example = "31926")
    private Long ownerUserId;

}
