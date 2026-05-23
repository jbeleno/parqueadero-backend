package com.usco.parqueaderos_api.ocr.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Se publica cuando el sidecar OCR lee una placa en el frame de una camara.
 * El listener emite PLACA_DETECTADA en /topic/parqueadero/{id} para que el
 * front decida la accion segun tipoCamara (ENTRADA/SALIDA/SEGURIDAD).
 */
public class PlacaDetectadaEvent extends ApplicationEvent {

    private final Long camaraId;
    private final Long parqueaderoId;
    private final String tipoCamara;     // "ENTRADA" | "SALIDA" | "SEGURIDAD"
    private final String placa;          // ej "KJV807"
    private final Double confianza;      // 0..1
    private final LocalDateTime detectedAt;

    // Resultado de la accion automatica de TicketAutoService.
    // accion: ENTRADA_REGISTRADA | ENTRADA_DUPLICADA | SALIDA_REGISTRADA
    //         | SALIDA_SIN_TICKET | SOLO_DETECCION | ERROR
    private final String accion;
    private final Long ticketId;            // null si no se afecto ningun ticket
    private final Long vehiculoId;          // null si no se resolvio veh
    private final Boolean vehiculoCreado;   // true si fue creado como invitado en esta entrada
    private final Long puntoParqueoId;      // null si no se asigno
    private final Double montoCalculado;    // solo en SALIDA_REGISTRADA
    private final String mensaje;

    public PlacaDetectadaEvent(Object source, Long camaraId, Long parqueaderoId,
                                String tipoCamara, String placa, Double confianza,
                                LocalDateTime detectedAt,
                                String accion, Long ticketId, Long vehiculoId,
                                Boolean vehiculoCreado, Long puntoParqueoId,
                                Double montoCalculado, String mensaje) {
        super(source);
        this.camaraId = camaraId;
        this.parqueaderoId = parqueaderoId;
        this.tipoCamara = tipoCamara;
        this.placa = placa;
        this.confianza = confianza;
        this.detectedAt = detectedAt;
        this.accion = accion;
        this.ticketId = ticketId;
        this.vehiculoId = vehiculoId;
        this.vehiculoCreado = vehiculoCreado;
        this.puntoParqueoId = puntoParqueoId;
        this.montoCalculado = montoCalculado;
        this.mensaje = mensaje;
    }

    public Long getCamaraId() { return camaraId; }
    public Long getParqueaderoId() { return parqueaderoId; }
    public String getTipoCamara() { return tipoCamara; }
    public String getPlaca() { return placa; }
    public Double getConfianza() { return confianza; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public String getAccion() { return accion; }
    public Long getTicketId() { return ticketId; }
    public Long getVehiculoId() { return vehiculoId; }
    public Boolean getVehiculoCreado() { return vehiculoCreado; }
    public Long getPuntoParqueoId() { return puntoParqueoId; }
    public Double getMontoCalculado() { return montoCalculado; }
    public String getMensaje() { return mensaje; }
}
