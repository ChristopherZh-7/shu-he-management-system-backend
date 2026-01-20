package cn.shuhe.system.module.system.api.dingtalk.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 钉钉通知发送请求 DTO
 */
@Data
@Accessors(chain = true)
public class DingtalkNotifySendReqDTO {

    /**
     * 接收人系统用户ID列表
     */
    @NotNull(message = "接收人用户ID列表不能为空")
    private List<Long> userIds;

    /**
     * 消息标题
     */
    @NotEmpty(message = "消息标题不能为空")
    private String title;

    /**
     * 消息内容（markdown格式）
     */
    @NotEmpty(message = "消息内容不能为空")
    private String content;

}
