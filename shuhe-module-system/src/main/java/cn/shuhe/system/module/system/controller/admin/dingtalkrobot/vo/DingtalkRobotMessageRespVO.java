package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 钉钉群机器人消息记录 Response VO")
@Data
public class DingtalkRobotMessageRespVO {

    @Schema(description = "消息编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "机器人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long robotId;

    @Schema(description = "机器人名称", example = "项目通知机器人")
    private String robotName;

    @Schema(description = "消息类型（text/markdown/actionCard/link）", requiredMode = Schema.RequiredMode.REQUIRED, example = "text")
    private String msgType;

    @Schema(description = "消息内容（JSON格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "@的手机号列表（JSON数组）")
    private String atMobiles;

    @Schema(description = "@的用户ID列表（JSON数组）")
    private String atUserIds;

    @Schema(description = "是否@所有人", example = "false")
    private Boolean isAtAll;

    @Schema(description = "发送状态（0-成功 1-失败）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer sendStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
