package com.usco.parqueaderos_api.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketCerradoEvent extends ApplicationEvent {

    private final Long ticketId;
    private final Long parqueaderoId;
    private final Long puntoParqueoId;

    public TicketCerradoEvent(Object source, Long ticketId, Long parqueaderoId, Long puntoParqueoId) {
        super(source);
        this.ticketId = ticketId;
        this.parqueaderoId = parqueaderoId;
        this.puntoParqueoId = puntoParqueoId;
    }
}
