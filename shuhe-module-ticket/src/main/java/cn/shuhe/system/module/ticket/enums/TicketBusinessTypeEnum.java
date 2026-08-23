package cn.shuhe.system.module.ticket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单业务类型枚举。对应 `shuhe_ticket.business_type` 字符串字段。
 *
 * <p>一期前端创建仅允许 {@link #GENERAL}；其它类型由各业务模块通过事件适配层落工单（二期）。
 */
@Getter
@AllArgsConstructor
public enum TicketBusinessTypeEnum {

    GENERAL("general", "通用工单"),
    OUTSIDE_REQUEST("outside_request", "外协请求"),
    SERVICE_LAUNCH("service_launch", "服务工单");

    private final String type;
    private final String name;

    public static TicketBusinessTypeEnum of(String type) {
        if (type == null) {
            return null;
        }
        for (TicketBusinessTypeEnum e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 创建工单允许的 business_type 白名单。
     *
     * <p>一期开放：{@link #GENERAL}（通用工单）+ {@link #SERVICE_LAUNCH}（服务派遣 / 借调申请，
     * 接单后由 {@code project} 模块的 listener 自动落 service_launch 业务表）。
     */
    public static boolean isWriteAllowed(String type) {
        return GENERAL.type.equals(type) || SERVICE_LAUNCH.type.equals(type);
    }
}
