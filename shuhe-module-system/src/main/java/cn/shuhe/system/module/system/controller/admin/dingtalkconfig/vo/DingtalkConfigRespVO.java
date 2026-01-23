package cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 钉钉配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DingtalkConfigRespVO {

    @Schema(description = "配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "446")
    @ExcelProperty("配置编号")
    private Long id;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("配置名称")
    private String name;

    @Schema(description = "企业ID（CorpId）", example = "28276")
    @ExcelProperty("企业ID（CorpId）")
    private String corpId;

    @Schema(description = "App ID", example = "2841")
    @ExcelProperty("App ID")
    private String appId;

    @Schema(description = "AgentId", requiredMode = Schema.RequiredMode.REQUIRED, example = "23001")
    @ExcelProperty("AgentId")
    private String agentId;

    @Schema(description = "Client ID（原AppKey）", requiredMode = Schema.RequiredMode.REQUIRED, example = "24430")
    @ExcelProperty("Client ID（原AppKey）")
    private String clientId;

    @Schema(description = "Client Secret（原AppSecret）", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("Client Secret（原AppSecret）")
    private String clientSecret;

    @Schema(description = "状态（0正常 1停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "最后同步时间")
    @ExcelProperty("最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "最后同步结果")
    @ExcelProperty("最后同步结果")
    private String lastSyncResult;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "回调基础URL（公网可访问域名）", example = "http://your-domain.com")
    @ExcelProperty("回调基础URL")
    private String callbackBaseUrl;

    @Schema(description = "钉钉OA外出申请流程编码", example = "PROC-XXXXXX")
    @ExcelProperty("OA流程编码")
    private String outsideProcessCode;

    @Schema(description = "默认外出类型", example = "因公外出")
    @ExcelProperty("默认外出类型")
    private String outsideType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
