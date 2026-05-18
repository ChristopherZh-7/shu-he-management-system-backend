package cn.shuhe.system.module.ticket.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * 工单操作日志 DO（{@code shuhe_ticket_log}）。
 *
 * <p>每次状态变更 / 分派 / 转交都会写一条；评论本身不写日志（在 {@link TicketCommentDO} 表）。
 */
@TableName(value = "shuhe_ticket_log", autoResultMap = true)
@KeySequence("shuhe_ticket_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 工单 ID。
     */
    private Long ticketId;

    /**
     * 操作人 ID。
     */
    private Long operatorId;

    /**
     * 操作人姓名快照。
     */
    private String operatorName;

    /**
     * 动作，{@link cn.shuhe.system.module.ticket.enums.TicketActionEnum#getAction()}。
     */
    private String action;

    /**
     * 变更前状态。
     */
    private Integer fromStatus;

    /**
     * 变更后状态。
     */
    private Integer toStatus;

    /**
     * 变更前处理人 ID。
     */
    private Long fromAssigneeId;

    /**
     * 变更后处理人 ID。
     */
    private Long toAssigneeId;

    /**
     * 操作说明 / 备注。
     */
    private String content;

    /**
     * 扩展字段（JSON）。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extJson;

}
