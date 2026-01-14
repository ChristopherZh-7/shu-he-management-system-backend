package cn.shuhe.system.module.ticket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态枚举
 */
@Getter
@AllArgsConstructor
public enum TicketStatusEnum {

    PENDING(0, "待处理"),
    ASSIGNED(1, "已分配"),
    PROCESSING(2, "处理中"),
    PENDING_CONFIRM(3, "待确认"),
    COMPLETED(4, "已完成"),
    CLOSED(5, "已关闭"),
    CANCELLED(6, "已取消");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

}
