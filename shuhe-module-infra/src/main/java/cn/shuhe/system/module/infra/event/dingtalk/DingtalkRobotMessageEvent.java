package cn.shuhe.system.module.infra.event.dingtalk;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 钉钉机器人消息事件
 * <p>
 * 当用户在群里@机器人发消息时发布此事件，
 * 业务模块（如 CRM 商机群）可监听并处理指令。
 *
 * @author shuhe
 */
@Getter
public class DingtalkRobotMessageEvent extends ApplicationEvent {

    /**
     * 群聊ID（钉钉 conversationId）
     */
    private final String chatId;

    /**
     * 发送者钉钉用户ID
     */
    private final String senderDingtalkUserId;

    /**
     * 消息内容
     */
    private final String content;

    public DingtalkRobotMessageEvent(Object source, String chatId, String senderDingtalkUserId, String content) {
        super(source);
        this.chatId = chatId;
        this.senderDingtalkUserId = senderDingtalkUserId;
        this.content = content;
    }

}
