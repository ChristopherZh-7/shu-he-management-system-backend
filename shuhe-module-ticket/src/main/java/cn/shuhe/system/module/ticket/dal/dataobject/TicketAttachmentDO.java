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
 * 工单附件 DO（{@code shuhe_ticket_attachment}）。
 *
 * <p>{@code commentId} 不为空时表示评论附件；为空时为工单本身附件。
 */
@TableName("shuhe_ticket_attachment")
@KeySequence("shuhe_ticket_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAttachmentDO extends BaseDO {

    @TableId
    private Long id;

    private Long ticketId;

    /**
     * 关联评论 ID（可选；为空表示工单本身附件）。
     */
    private Long commentId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    /**
     * MIME 类型。
     */
    private String fileType;

    private Long uploaderId;

}
