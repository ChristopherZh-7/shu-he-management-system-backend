package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 钉钉通知发送日志 Response VO")
@Data
public class DingtalkNotificationLogRespVO {

    @Schema(description = "日志编号", example = "1")
    private Long id;

    @Schema(description = "配置ID", example = "1")
    private Long configId;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "机器人ID", example = "1")
    private Long robotId;

    @Schema(description = "机器人名称")
    private String robotName;

    @Schema(description = "事件类型", example = "contract_create")
    private String eventType;

    @Schema(description = "事件模块", example = "crm")
    private String eventModule;

    @Schema(description = "业务数据ID", example = "1")
    private Long businessId;

    @Schema(description = "业务编号", example = "HT202602050001")
    private String businessNo;

    @Schema(description = "发送的标题")
    private String title;

    @Schema(description = "发送的内容")
    private String content;

    @Schema(description = "@的手机号")
    private String atMobiles;

    @Schema(description = "发送状态（0-成功 1-失败）", example = "0")
    private Integer sendStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
