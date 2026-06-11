package cn.shuhe.system.module.ticket.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工单状态枚举。
 *
 * <p>对应 `shuhe_ticket.status` 字段；详细状态机见
 * {@code docs/design/ticket-design.md} §1。
 */
@Getter
@AllArgsConstructor
public enum TicketStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待处理"),
    IN_PROGRESS(1, "处理中"),
    PENDING_REVIEW(2, "待验收"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭"),
    CANCELLED(5, "已取消"),
    RETURNED(6, "已退回");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TicketStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TicketStatusEnum of(Integer status) {
        if (status == null) {
            return null;
        }
        for (TicketStatusEnum e : values()) {
            if (e.status.equals(status)) {
                return e;
            }
        }
        return null;
    }

    public static String nameOf(Integer status) {
        TicketStatusEnum e = of(status);
        return e == null ? null : e.name;
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }
}
