package cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉群机器人消息记录 DO
 * 
 * 记录机器人发送的消息历史，便于追踪和排查问题
 *
 * @author shuhe
 */
@TableName("system_dingtalk_robot_message")
@KeySequence("system_dingtalk_robot_message_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkRobotMessageDO extends BaseDO {

    /**
     * 消息编号
     */
    @TableId
    private Long id;

    /**
     * 机器人编号
     */
    private Long robotId;

    /**
     * 消息类型
     * text-文本消息
     * markdown-Markdown消息
     * actionCard-卡片消息
     * link-链接消息
     */
    private String msgType;

    /**
     * 消息内容（JSON格式）
     */
    private String content;

    /**
     * @的手机号列表（JSON数组）
     */
    private String atMobiles;

    /**
     * @的用户ID列表（JSON数组）
     */
    private String atUserIds;

    /**
     * 是否@所有人
     */
    private Boolean isAtAll;

    /**
     * 发送状态（0-成功 1-失败）
     */
    private Integer sendStatus;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 钉钉返回数据
     */
    private String responseData;

}
