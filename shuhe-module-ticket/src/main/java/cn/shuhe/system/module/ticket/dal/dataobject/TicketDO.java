package cn.shuhe.system.module.ticket.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工单 DO
 */
@TableName("ticket_ticket")
@KeySequence("ticket_ticket_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDO extends BaseDO {

    /**
     * 工单ID
     */
    @TableId
    private Long id;
    
    /**
     * 工单编号（如：TK202601150001）
     */
    private String ticketNo;
    
    /**
     * 工单标题
     */
    private String title;
    
    /**
     * 工单描述
     */
    private String description;
    
    /**
     * 工单分类ID
     */
    private Long categoryId;
    
    /**
     * 优先级：0-低 1-普通 2-高 3-紧急
     */
    private Integer priority;
    
    /**
     * 状态：0-待处理 1-已分配 2-处理中 3-待确认 4-已完成 5-已关闭 6-已取消
     */
    private Integer status;
    
    /**
     * 创建人ID（发起人）
     */
    private Long creatorId;
    
    /**
     * 处理人ID
     */
    private Long assigneeId;
    
    /**
     * 关联客户ID（可选）
     */
    private Long customerId;
    
    /**
     * 期望完成时间
     */
    private LocalDateTime expectTime;
    
    /**
     * 实际完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 附件URL（JSON数组）
     */
    private String attachments;
    
    /**
     * 备注
     */
    private String remark;

}
