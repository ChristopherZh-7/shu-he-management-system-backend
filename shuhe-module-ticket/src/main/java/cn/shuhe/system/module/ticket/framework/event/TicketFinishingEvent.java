package cn.shuhe.system.module.ticket.framework.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Business completion constraints apply equally to manual API calls. */
@Getter
@RequiredArgsConstructor
public class TicketFinishingEvent {
    private final Long ticketId;
}
