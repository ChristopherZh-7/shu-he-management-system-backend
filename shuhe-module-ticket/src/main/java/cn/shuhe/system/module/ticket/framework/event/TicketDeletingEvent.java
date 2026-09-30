package cn.shuhe.system.module.ticket.framework.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TicketDeletingEvent {
    private final Long ticketId;
}
