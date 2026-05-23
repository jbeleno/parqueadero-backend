package com.usco.parqueaderos_api.ocr;

import com.usco.parqueaderos_api.common.event.CamaraImagenActualizadaEvent;
import com.usco.parqueaderos_api.common.event.PlacaDetectadaEvent;
import com.usco.parqueaderos_api.common.storage.ImageStorageService;
import com.usco.parqueaderos_api.parking.entity.Camara;
import com.usco.parqueaderos_api.parking.entity.TipoCamara;
import com.usco.parqueaderos_api.parking.repository.CamaraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Cuando una camara recibe un frame nuevo, dispara la lectura de placa
 * contra el sidecar OCR en background. Si detecta una o mas placas
 * publica PlacaDetectadaEvent por cada una (el listener de notificaciones
 * los convierte en WS PLACA_DETECTADA).
 *
 * Async para no bloquear el response del POST /imagen: el operador
 * sube la foto y devuelve 200 inmediato; las lecturas llegan via WS
 * 2-3 segundos despues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OcrEventListener {

    private final PlateReaderClient plateReader;
    private final CamaraRepository camaraRepo;
    private final ImageStorageService imageStorage;
    private final ApplicationEventPublisher publisher;
    private final TicketAutoService ticketAutoService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onImagenActualizada(CamaraImagenActualizadaEvent event) {
        // AFTER_COMMIT: solo se ejecuta si la transaccion que persistio la imagen
        // hizo commit exitoso. Evita procesar imagenes fantasma en caso de rollback.
        // No @Transactional aqui: TicketAutoService.procesarPlacaDetectada abre la
        // suya propia (REQUIRED) y los INSERT funcionan sin heredar readOnly.
        if (event.getCamaraId() == null) return;

        Camara camara = camaraRepo.findById(event.getCamaraId()).orElse(null);
        if (camara == null) {
            log.debug("OCR skip: camara {} no existe", event.getCamaraId());
            return;
        }
        if (camara.getImagenPath() == null) {
            log.debug("OCR skip: camara {} sin imagenPath", event.getCamaraId());
            return;
        }

        byte[] bytes;
        try {
            bytes = imageStorage.read("camaras/" + camara.getId());
        } catch (Exception e) {
            log.warn("OCR: no se pudo leer la imagen de camara {}: {}",
                    camara.getId(), e.getMessage());
            return;
        }

        var placas = plateReader.readPlates(
                camara.getId(), bytes, camara.getImagenContentType());

        if (placas.isEmpty()) {
            log.debug("OCR: camara {} sin placas detectadas", camara.getId());
            return;
        }

        String tipoCamaraStr = camara.getTipo() != null
                ? camara.getTipo().name() : TipoCamara.SEGURIDAD.name();
        LocalDateTime now = LocalDateTime.now();

        for (var p : placas) {
            log.info("OCR: camara {} ({}) detecto placa {} conf={}",
                    camara.getId(), tipoCamaraStr, p.placa(), p.confianza());

            // Aplicar logica de negocio (crear/cerrar ticket auto)
            TicketAutoService.AutoActionResult result = ticketAutoService.procesarPlacaDetectada(
                    camara.getId(), event.getParqueaderoId(), tipoCamaraStr, p.placa());

            log.info("OCR: accion={} ticket={} vehiculo={} mensaje={}",
                    result.accion(), result.ticketId(), result.vehiculoId(), result.mensaje());

            publisher.publishEvent(new PlacaDetectadaEvent(
                    this,
                    camara.getId(),
                    event.getParqueaderoId(),
                    tipoCamaraStr,
                    p.placa(),
                    p.confianza(),
                    now,
                    result.accion().name(),
                    result.ticketId(),
                    result.vehiculoId(),
                    result.vehiculoCreado(),
                    result.puntoParqueoId(),
                    result.montoCalculado(),
                    result.mensaje()
            ));
        }
    }
}
