package cn.shuhe.system.module.crm.controller.admin.contract.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - CRM 合同附件更新 Request VO")
@Data
public class CrmContractAttachmentUpdateReqVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "合同编号不能为空")
    private Long id;

    @Schema(description = "合同附件URL", example = "https://xxx.com/file.pdf")
    private String attachment;
}
