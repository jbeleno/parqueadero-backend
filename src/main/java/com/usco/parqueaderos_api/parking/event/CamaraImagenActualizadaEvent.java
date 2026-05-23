package com.usco.parqueaderos_api.parking.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Se publica cuando una camara recibe una nueva imagen. El listener emite
 * CAMERA_IMAGE_UPDATED en /topic/parqueadero/{id}.
 *
 * Nota: ApplicationEvent.getTimestamp() ya existe y es final. Por eso
 * el campo de timestamp del frame se llama "imagenTimestamp" para evitar
 * el conflicto en la herencia.
 */
public class CamaraImagenActualizadaEvent extends ApplicationEvent {

    private final Long camaraId;
    private final Long parqueaderoId;
    private final LocalDateTime imagenTimestamp;

    public CamaraImagenActualizadaEvent(Object source, Long camaraId,
                                        Long parqueaderoId, LocalDateTime imagenTimestamp) {
        super(source);
        this.camaraId = camaraId;
        this.parqueaderoId = parqueaderoId;
        this.imagenTimestamp = imagenTimestamp;
    }

    public Long getCamaraId() { return camaraId; }
    public Long getParqueaderoId() { return parqueaderoId; }
    public LocalDateTime getImagenTimestamp() { return imagenTimestamp; }
}
