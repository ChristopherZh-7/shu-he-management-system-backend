package cn.shuhe.system.module.ticket.framework.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Map;

/** Synchronous validation by business modules, before persisting user input. */
@Getter
@RequiredArgsConstructor
public class TicketSavingEvent {
    private final String businessType;
    private final Map<String, Object> extJson;
}
