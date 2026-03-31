package cn.shuhe.system.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 钉钉 JSAPI 签名（dd.config） Response VO")
@Data
public class AuthDingtalkJsapiConfigRespVO {

    @Schema(description = "企业 CorpId", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpId;

    @Schema(description = "微应用 AgentId（字符串，前端可转数字）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String agentId;

    @Schema(description = "时间戳（秒，字符串）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String timeStamp;

    @Schema(description = "随机串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nonceStr;

    @Schema(description = "签名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signature;
}
