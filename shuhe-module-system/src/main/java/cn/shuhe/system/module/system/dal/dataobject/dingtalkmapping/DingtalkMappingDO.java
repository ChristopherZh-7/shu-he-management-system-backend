package cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;

/**
 * 钉钉数据映射 DO
 *
 * @author 芋道源码
 */
@TableName("system_dingtalk_mapping")
@KeySequence("system_dingtalk_mapping_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DingtalkMappingDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 钉钉配置ID
     */
    private Long configId;
    /**
     * 类型（DEPT-部门，USER-用户）
     */
    private String type;
    /**
     * 本地ID
     */
    private Long localId;
    /**
     * 钉钉ID
     */
    private String dingtalkId;


}