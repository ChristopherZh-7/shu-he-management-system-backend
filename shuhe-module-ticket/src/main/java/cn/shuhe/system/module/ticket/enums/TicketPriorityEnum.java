package cn.shuhe.system.module.ticket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单优先级枚举
 */
@Getter
@AllArgsConstructor
public enum TicketPriorityEnum {

    LOW(0, "低"),
    NORMAL(1, "普通"),
    HIGH(2, "高"),
    URGENT(3, "紧急");

    /**
     * 优先级值
     */
    private final Integer priority;
    /**
     * 优先级名
     */
    private final String name;

}
