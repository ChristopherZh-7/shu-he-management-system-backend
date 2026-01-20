package cn.shuhe.system.module.system.dal.dataobject.cost;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 职级变更记录 DO
 */
@TableName("system_position_level_history")
@KeySequence("system_position_level_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionLevelHistoryDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 变更前职级
     */
    private String oldPositionLevel;

    /**
     * 变更后职级
     */
    private String newPositionLevel;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 变更类型：1自动同步 2手动录入
     */
    private Integer changeType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 变更类型枚举
     */
    public static final int CHANGE_TYPE_AUTO = 1;
    public static final int CHANGE_TYPE_MANUAL = 2;

}
