package cn.shuhe.system.module.ticket.framework.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工单履约状态变更事件。
 *
 * <p>由 ticket 模块同步发布，业务模块按 {@link #businessType} 过滤后同步自己的执行记录。
 * 监听器抛出异常时，工单状态变更与业务记录一起回滚，避免两边状态分裂。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLifecycleEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private String ticketNo;
    private String businessType;
    private Long businessId;
    private String action;
    private String result;
}
