package com.usco.parqueaderos_api.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * Se publica cuando un ticket EN_CURSO cambia de punto de parqueo
 * (operador lo mueve desde el front porque el carro se estaciono en otro
 * cubiculo distinto al asignado automaticamente por OCR).
 *
 * El listener emite dos SPOT_STATUS_CHANGE (viejo = free, nuevo = occupied)
 * y un OCUPACION_ACTUALIZADA en /topic/parqueadero/{id}.
 */
public class TicketPuntoCambiadoEvent extends ApplicationEvent {

    private final Long ticketId;
    private final Long parqueaderoId;
    private final Long puntoParqueoAnteriorId;
    private final Long puntoParqueoNuevoId;

    public TicketPuntoCambiadoEvent(Object source, Long ticketId, Long parqueaderoId,
                                     Long puntoParqueoAnteriorId, Long puntoParqueoNuevoId) {
        super(source);
        this.ticketId = ticketId;
        this.parqueaderoId = parqueaderoId;
        this.puntoParqueoAnteriorId = puntoParqueoAnteriorId;
        this.puntoParqueoNuevoId = puntoParqueoNuevoId;
    }

    public Long getTicketId() { return ticketId; }
    public Long getParqueaderoId() { return parqueaderoId; }
    public Long getPuntoParqueoAnteriorId() { return puntoParqueoAnteriorId; }
    public Long getPuntoParqueoNuevoId() { return puntoParqueoNuevoId; }
}
