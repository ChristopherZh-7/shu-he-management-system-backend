package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "管理后台 - 钉钉群机器人发送消息 Request VO")
@Data
public class DingtalkRobotSendReqVO {

    @Schema(description = "机器人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "机器人编号不能为空")
    private Long robotId;

    @Schema(description = "消息类型（text/markdown/link/actionCard）", requiredMode = Schema.RequiredMode.REQUIRED, example = "text")
    @NotBlank(message = "消息类型不能为空")
    private String msgType;

    @Schema(description = "标题（markdown/link/actionCard类型必填）", example = "项目通知")
    private String title;

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "这是一条测试消息")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "@的手机号列表", example = "[\"138xxxx1234\",\"139xxxx5678\"]")
    private List<String> atMobiles;

    @Schema(description = "@的用户ID列表", example = "[\"user001\",\"user002\"]")
    private List<String> atUserIds;

    @Schema(description = "是否@所有人", example = "false")
    private Boolean isAtAll;

    @Schema(description = "跳转链接（link/actionCard类型使用）", example = "https://example.com")
    private String messageUrl;

    @Schema(description = "图片链接（link类型使用）", example = "https://example.com/pic.png")
    private String picUrl;

    @Schema(description = "按钮文字（actionCard类型使用）", example = "查看详情")
    private String singleTitle;

}
