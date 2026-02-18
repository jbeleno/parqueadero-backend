package com.usco.parqueaderos_api.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketCreadoEvent extends ApplicationEvent {

    private final Long ticketId;
    private final Long parqueaderoId;
    private final Long puntoParqueoId;

    public TicketCreadoEvent(Object source, Long ticketId, Long parqueaderoId, Long puntoParqueoId) {
        super(source);
        this.ticketId = ticketId;
        this.parqueaderoId = parqueaderoId;
        this.puntoParqueoId = puntoParqueoId;
    }
}
