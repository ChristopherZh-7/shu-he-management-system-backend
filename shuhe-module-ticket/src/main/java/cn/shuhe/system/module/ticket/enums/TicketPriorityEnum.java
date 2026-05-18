package cn.shuhe.system.module.ticket.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工单优先级枚举。对应 `shuhe_ticket.priority`。
 */
@Getter
@AllArgsConstructor
public enum TicketPriorityEnum implements ArrayValuable<Integer> {

    LOW(0, "低"),
    NORMAL(1, "中"),
    HIGH(2, "高"),
    URGENT(3, "紧急");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TicketPriorityEnum::getPriority).toArray(Integer[]::new);

    private final Integer priority;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TicketPriorityEnum of(Integer priority) {
        if (priority == null) {
            return null;
        }
        for (TicketPriorityEnum e : values()) {
            if (e.priority.equals(priority)) {
                return e;
            }
        }
        return null;
    }
}
