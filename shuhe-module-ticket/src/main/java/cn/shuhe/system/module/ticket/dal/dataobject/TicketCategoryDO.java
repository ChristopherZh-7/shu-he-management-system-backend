package cn.shuhe.system.module.ticket.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 工单分类 DO（{@code shuhe_ticket_category}，树形）。
 */
@TableName("shuhe_ticket_category")
@KeySequence("shuhe_ticket_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCategoryDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 父分类 ID，0 表示顶级。
     */
    private Long parentId;

    /**
     * 分类名称（同 parentId 下唯一）。
     */
    private String name;

    /**
     * 分类编码（全局唯一，可选）。
     */
    private String code;

    /**
     * 图标。
     */
    private String icon;

    /**
     * 排序。
     */
    private Integer sort;

    /**
     * 默认处理人 ID。
     */
    private Long defaultAssigneeId;

    /**
     * 默认处理部门 ID。
     */
    private Long defaultAssigneeDeptId;

    /**
     * 默认优先级。
     */
    private Integer defaultPriority;

    /**
     * 默认 SLA 小时数。
     */
    private Integer defaultSlaHours;

    /**
     * 状态：0=启用 / 1=禁用。
     */
    private Integer status;

}
