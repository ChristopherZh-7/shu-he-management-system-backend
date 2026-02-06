package cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉通知发送日志 DO
 * 
 * 记录每次自动发送的结果
 *
 * @author shuhe
 */
@TableName("system_dingtalk_notification_log")
@KeySequence("system_dingtalk_notification_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkNotificationLogDO extends BaseDO {

    /**
     * 日志编号
     */
    @TableId
    private Long id;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 机器人ID
     */
    private Long robotId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件模块
     */
    private String eventModule;

    /**
     * 业务数据ID（如合同ID）
     */
    private Long businessId;

    /**
     * 业务编号（如合同编号）
     */
    private String businessNo;

    /**
     * 发送的标题
     */
    private String title;

    /**
     * 发送的内容
     */
    private String content;

    /**
     * @的手机号
     */
    private String atMobiles;

    /**
     * 发送状态（0-成功 1-失败）
     */
    private Integer sendStatus;

    /**
     * 错误信息
     */
    private String errorMsg;

}
