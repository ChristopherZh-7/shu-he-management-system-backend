package cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.shuhe.system.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉配置分页 Request VO")
@Data
public class DingtalkConfigPageReqVO extends PageParam {

    @Schema(description = "配置名称", example = "李四")
    private String name;

    @Schema(description = "企业ID（CorpId）", example = "28276")
    private String corpId;

    @Schema(description = "App ID", example = "2841")
    private String appId;

    @Schema(description = "AgentId", example = "23001")
    private String agentId;

    @Schema(description = "Client ID（原AppKey）", example = "24430")
    private String clientId;

    @Schema(description = "Client Secret（原AppSecret）")
    private String clientSecret;

    @Schema(description = "状态（0正常 1停用）", example = "1")
    private Integer status;

    @Schema(description = "最后同步时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] lastSyncTime;

    @Schema(description = "最后同步结果")
    private String lastSyncResult;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}