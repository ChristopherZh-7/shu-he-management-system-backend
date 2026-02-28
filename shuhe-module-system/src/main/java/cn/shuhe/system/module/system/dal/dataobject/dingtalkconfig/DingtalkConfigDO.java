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
     * 钉钉OA外出申请流程编码
     */
    private String outsideProcessCode;

    /**
     * 默认外出类型
     */
    private String outsideType;


}
