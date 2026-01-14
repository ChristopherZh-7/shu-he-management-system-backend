package cn.shuhe.system.module.ticket.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工单操作日志 DO
 */
@TableName("ticket_log")
@KeySequence("ticket_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLogDO extends BaseDO {

    /**
     * 日志ID
     */
    @TableId
    private Long id;
    
    /**
     * 工单ID
     */
    private Long ticketId;
    
    /**
     * 操作类型：create-创建 assign-分配 process-处理 finish-完成 close-关闭 cancel-取消 comment-评论
     */
    private String action;
    
    /**
     * 操作内容/备注
     */
    private String content;
    
    /**
     * 操作人ID
     */
    private Long operatorId;

}
