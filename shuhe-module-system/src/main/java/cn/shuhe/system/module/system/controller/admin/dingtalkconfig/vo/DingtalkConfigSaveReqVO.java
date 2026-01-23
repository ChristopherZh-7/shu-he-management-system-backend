package cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 钉钉配置新增/修改 Request VO")
@Data
public class DingtalkConfigSaveReqVO {

    @Schema(description = "配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "446")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "配置名称不能为空")
    private String name;

    @Schema(description = "企业ID（CorpId）", example = "28276")
    private String corpId;

    @Schema(description = "App ID", example = "2841")
    private String appId;

    @Schema(description = "AgentId", requiredMode = Schema.RequiredMode.REQUIRED, example = "23001")
    @NotEmpty(message = "AgentId不能为空")
    private String agentId;

    @Schema(description = "Client ID（原AppKey）", requiredMode = Schema.RequiredMode.REQUIRED, example = "24430")
    @NotEmpty(message = "Client ID（原AppKey）不能为空")
    private String clientId;

    @Schema(description = "Client Secret（原AppSecret）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Client Secret（原AppSecret）不能为空")
    private String clientSecret;

    @Schema(description = "状态（0正常 1停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "最后同步结果")
    private String lastSyncResult;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "回调基础URL（公网可访问域名）", example = "http://your-domain.com")
    private String callbackBaseUrl;

    @Schema(description = "钉钉OA外出申请流程编码", example = "PROC-XXXXXX")
    private String outsideProcessCode;

    @Schema(description = "默认外出类型", example = "因公外出")
    private String outsideType;

}