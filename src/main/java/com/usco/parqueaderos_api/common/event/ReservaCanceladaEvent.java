package com.usco.parqueaderos_api.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Reserva pasada a CANCELADA / EXPIRADA o eliminada. El punto asociado
 * (si lo tenia) queda libre y el frontend debe refrescar su estado.
 */
@Getter
public class ReservaCanceladaEvent extends ApplicationEvent {

    private final Long reservaId;
    private final Long usuarioId;
    private final Long parqueaderoId;
    private final Long puntoParqueoId;

    public ReservaCanceladaEvent(Object source, Long reservaId, Long usuarioId,
                                  Long parqueaderoId, Long puntoParqueoId) {
        super(source);
        this.reservaId = reservaId;
        this.usuarioId = usuarioId;
        this.parqueaderoId = parqueaderoId;
        this.puntoParqueoId = puntoParqueoId;
    }
}
