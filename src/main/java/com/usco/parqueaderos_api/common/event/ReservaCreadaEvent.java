package com.usco.parqueaderos_api.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReservaCreadaEvent extends ApplicationEvent {

    private final Long reservaId;
    private final Long usuarioId;
    private final Long parqueaderoId;

    public ReservaCreadaEvent(Object source, Long reservaId, Long usuarioId, Long parqueaderoId) {
        super(source);
        this.reservaId = reservaId;
        this.usuarioId = usuarioId;
        this.parqueaderoId = parqueaderoId;
    }
}
