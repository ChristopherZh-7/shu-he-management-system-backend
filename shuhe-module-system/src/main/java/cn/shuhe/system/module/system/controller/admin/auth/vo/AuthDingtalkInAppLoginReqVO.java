package cn.shuhe.system.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - 钉钉端内免登（JSAPI authCode） Request VO")
@Data
public class AuthDingtalkInAppLoginReqVO {

    @Schema(description = "dd.runtime.permission.requestAuthCode 返回的临时码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "authCode 不能为空")
    private String authCode;
}
