package cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉配置 DO
 *
 * @author ShuHe
 */
@TableName("system_dingtalk_config")
@KeySequence("system_dingtalk_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkConfigDO extends BaseDO {

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
     * 企业ID（CorpId）
     */
    private String corpId;
    /**
     * App ID
     */
    private String appId;
    /**
     * AgentId
     */
    private String agentId;
    /**
     * Client ID（原AppKey）
     */
    private String clientId;
    /**
     * Client Secret（原AppSecret）
     */
    private String clientSecret;
    /**
     * 状态（0正常 1停用）
     */
    private Integer status;
    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;
    /**
     * 最后同步结果
     */
    private String lastSyncResult;
    /**
     * 备注
     */
    private String remark;

    /**
     * 回调基础URL（公网可访问域名）
     */
    private String callbackBaseUrl;

    /**
     * 审批链接 baseUrl（前端/网关入口，如 http://localhost:5666）
     * 不填则用 callbackBaseUrl。用于生成「通过审批」「驳回」「修改金额」等链接
     */
    private String approveBaseUrl;

    /**
     * 钉钉OA外出申请流程编码
     */
    private String outsideProcessCode;

    /**
     * 默认外出类型
     */
    private String outsideType;

    /**
     * 商机审批固定群 chatId
     * 配置后不再每个商机建群，所有审批通知发到此群。需手动建群并加应用机器人一次。
     */
    private String businessAuditChatId;

    /**
     * 商机审批场景群模板ID
     * 配置后使用场景群API建群，机器人自动进群，无需每次手动添加。
     */
    private String businessAuditTemplateId;

}
