package cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉群机器人配置 DO
 * 
 * 支持通过Webhook方式向钉钉群发送消息，并支持@指定人员
 *
 * @author shuhe
 */
@TableName("system_dingtalk_robot")
@KeySequence("system_dingtalk_robot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkRobotDO extends BaseDO {

    /**
     * 机器人编号
     */
    @TableId
    private Long id;

    /**
     * 机器人名称
     */
    private String name;

    /**
     * Webhook地址
     */
    private String webhookUrl;

    /**
     * 加签密钥（安全设置为加签时必填）
     */
    private String secret;

    /**
     * 安全类型
     * 1-关键词
     * 2-加签
     * 3-IP白名单
     */
    private Integer securityType;

    /**
     * 关键词列表（JSON数组，安全类型为关键词时使用）
     */
    private String keywords;

    /**
     * 状态（0-正常 1-停用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
