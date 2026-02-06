package cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉通知场景配置 DO
 * 
 * 配置业务事件触发自动发送钉钉群消息的规则
 *
 * @author shuhe
 */
@TableName("system_dingtalk_notification_config")
@KeySequence("system_dingtalk_notification_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkNotificationConfigDO extends BaseDO {

    /**
     * 配置编号
     */
    @TableId
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 关联的机器人ID
     */
    private Long robotId;

    /**
     * 事件类型
     * 如：contract_create, contract_update, project_create 等
     */
    private String eventType;

    /**
     * 事件所属模块
     * 如：crm, project, bpm 等
     */
    private String eventModule;

    /**
     * 消息类型
     * text/markdown/link/actionCard
     */
    private String msgType;

    /**
     * 标题模板（支持变量如${contractName}）
     */
    private String titleTemplate;

    /**
     * 内容模板（支持变量替换）
     */
    private String contentTemplate;

    /**
     * @类型
     * 0-不@任何人
     * 1-@负责人
     * 2-@创建人
     * 3-@指定人员
     * 4-@所有人
     */
    private Integer atType;

    /**
     * @的手机号列表（JSON数组，atType=3时使用）
     */
    private String atMobiles;

    /**
     * @的用户ID列表（JSON数组，atType=3时使用）
     */
    private String atUserIds;

    /**
     * 状态（0-启用 1-停用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
