package cn.shuhe.system.module.ticket.enums;

import cn.shuhe.system.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 工单来源枚举。对应 `shuhe_ticket.source`。
 *
 * <p>一期前端创建均填 {@link #MANUAL}；其余来源由各业务模块通过事件适配层注入（二期）。
 */
@Getter
@AllArgsConstructor
public enum TicketSourceEnum implements ArrayValuable<Integer> {

    MANUAL(0, "手动创建"),
    OUTSIDE_REQUEST(1, "外协请求"),
    SERVICE_LAUNCH(2, "服务派遣"),
    API(3, "API 调用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TicketSourceEnum::getSource).toArray(Integer[]::new);

    private final Integer source;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static TicketSourceEnum of(Integer source) {
        if (source == null) {
            return null;
        }
        for (TicketSourceEnum e : values()) {
            if (e.source.equals(source)) {
                return e;
            }
        }
        return null;
    }
}
