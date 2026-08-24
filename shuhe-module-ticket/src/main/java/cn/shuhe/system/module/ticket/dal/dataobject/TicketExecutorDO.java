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
 * 工单执行人 DO（{@code shuhe_ticket_executor}）。
 *
 * <p>记录主管 {@code assignee_id} 接单后指派的多个实际执行人。{@code status} 字段允许执行人独立标记自身
 * 进度（一期 Service 暂未对外暴露，预留二期使用）。{@code uk_ticket_user} 唯一索引（含 deleted）防止重复
 * 分派同一执行人。
 */
@TableName(value = "shuhe_ticket_executor")
@KeySequence("shuhe_ticket_executor_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketExecutorDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 关联工单 ID。
     */
    private Long ticketId;

    /**
     * 执行人用户 ID。
     */
    private Long userId;

    /**
     * 执行人姓名快照。
     */
    private String userName;

    /**
     * 执行人部门 ID 快照。
     */
    private Long userDeptId;

    /** primary_executor / collaborator / tech_reviewer。 */
    private String roleType;

    /** 本人在本工单中的负责内容。 */
    private String responsibility;

    /**
     * 状态：0=执行中 / 1=已完成 / 2=已退出。
     */
    private Integer status;

    /** pending / working / submitted / completed / exited。 */
    private String taskStatus;

    /**
     * 分派人（接单的主管）ID。
     */
    private Long assignedBy;

    /**
     * 分派备注。
     */
    private String remark;

    public static final String ROLE_PRIMARY_EXECUTOR = "primary_executor";
    public static final String ROLE_COLLABORATOR = "collaborator";
    public static final String ROLE_TECH_REVIEWER = "tech_reviewer";

}
