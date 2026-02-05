package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 钉钉群机器人新增/修改 Request VO")
@Data
public class DingtalkRobotSaveReqVO {

    @Schema(description = "机器人编号", requiredMode = Schema.RequiredMode.AUTO, example = "1")
    private Long id;

    @Schema(description = "机器人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目通知机器人")
    @NotBlank(message = "机器人名称不能为空")
    private String name;

    @Schema(description = "Webhook地址", requiredMode = Schema.RequiredMode.REQUIRED, 
            example = "https://oapi.dingtalk.com/robot/send?access_token=xxx")
    @NotBlank(message = "Webhook地址不能为空")
    private String webhookUrl;

    @Schema(description = "加签密钥（安全设置为加签时必填）", example = "SECxxx")
    private String secret;

    @Schema(description = "安全类型（1-关键词 2-加签 3-IP白名单）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "安全类型不能为空")
    private Integer securityType;

    @Schema(description = "关键词列表（JSON数组，安全类型为关键词时使用）", example = "[\"通知\",\"提醒\"]")
    private String keywords;

    @Schema(description = "状态（0-正常 1-停用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "用于项目进度通知")
    private String remark;

}
