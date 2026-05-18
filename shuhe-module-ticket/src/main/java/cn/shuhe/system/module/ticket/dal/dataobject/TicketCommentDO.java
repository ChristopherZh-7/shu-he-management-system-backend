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
 * 工单评论 DO（{@code shuhe_ticket_comment}）。
 *
 * <p>{@code isInternal=true} 的内部评论提单人看不到（Service 层过滤）。
 */
@TableName("shuhe_ticket_comment")
@KeySequence("shuhe_ticket_comment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCommentDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 工单 ID。
     */
    private Long ticketId;

    /**
     * 评论人 ID。
     */
    private Long userId;

    /**
     * 评论人姓名快照。
     */
    private String userName;

    /**
     * 评论人部门 ID 快照。
     */
    private Long userDeptId;

    /**
     * 回复的父评论 ID（可选）。
     */
    private Long parentId;

    /**
     * 评论内容。
     */
    private String content;

    /**
     * 是否内部评论；提单人看不到。
     */
    private Boolean isInternal;

}
