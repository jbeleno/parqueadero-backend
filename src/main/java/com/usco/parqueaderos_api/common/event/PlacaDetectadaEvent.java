package com.usco.parqueaderos_api.common.event;

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

    public PlacaDetectadaEvent(Object source, Long camaraId, Long parqueaderoId,
                                String tipoCamara, String placa, Double confianza,
                                LocalDateTime detectedAt) {
        super(source);
        this.camaraId = camaraId;
        this.parqueaderoId = parqueaderoId;
        this.tipoCamara = tipoCamara;
        this.placa = placa;
        this.confianza = confianza;
        this.detectedAt = detectedAt;
    }

    public Long getCamaraId() { return camaraId; }
    public Long getParqueaderoId() { return parqueaderoId; }
    public String getTipoCamara() { return tipoCamara; }
    public String getPlaca() { return placa; }
    public Double getConfianza() { return confianza; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
