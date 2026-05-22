package com.usco.parqueaderos_api.notification.listener;

import com.usco.parqueaderos_api.common.event.CamaraImagenActualizadaEvent;
import com.usco.parqueaderos_api.common.event.PlacaDetectadaEvent;
import com.usco.parqueaderos_api.common.event.ReservaCanceladaEvent;
import com.usco.parqueaderos_api.common.event.ReservaCreadaEvent;
import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.parking.dto.DisponibilidadDTO;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.parking.service.DisponibilidadService;
import com.usco.parqueaderos_api.reservation.entity.Reserva;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final DisponibilidadService disponibilidadService;
    private final PuntoParqueoRepository puntoParqueoRepository;
    private final ReservaRepository reservaRepository;

    @Async
    @EventListener
    public void onTicketCreado(TicketCreadoEvent event) {
        if (event.getParqueaderoId() == null) return;
        NotificacionDTO notif = NotificacionDTO.builder()
                .tipo("TICKET_CREADO")
                .mensaje("Nuevo ticket creado en el parqueadero")
                .referenciaId(event.getTicketId())
                .parqueaderoId(event.getParqueaderoId())
                .build();
        notificationService.notificarParqueadero(event.getParqueaderoId(), notif);

        emitirSpotStatusChange(event.getPuntoParqueoId(), "occupied",
                event.getTicketId(), event.getParqueaderoId());
        emitirOcupacionActualizada(event.getParqueaderoId());
    }

    @Async
    @EventListener
    public void onTicketCerrado(TicketCerradoEvent event) {
        if (event.getParqueaderoId() == null) return;
        NotificacionDTO notif = NotificacionDTO.builder()
                .tipo("TICKET_CERRADO")
                .mensaje("Ticket cerrado — punto de parqueo liberado")
                .referenciaId(event.getTicketId())
                .parqueaderoId(event.getParqueaderoId())
                .build();
        notificationService.notificarParqueadero(event.getParqueaderoId(), notif);

        emitirSpotStatusChange(event.getPuntoParqueoId(), "free",
                event.getTicketId(), event.getParqueaderoId());
        emitirOcupacionActualizada(event.getParqueaderoId());
    }

    @Async
    @EventListener
    public void onReservaCreada(ReservaCreadaEvent event) {
        if (event.getUsuarioId() != null) {
            NotificacionDTO notif = NotificacionDTO.builder()
                    .tipo("RESERVA_CREADA")
                    .mensaje("Tu reserva fue creada exitosamente")
                    .referenciaId(event.getReservaId())
                    .parqueaderoId(event.getParqueaderoId())
                    .build();
            notificationService.notificarUsuario(event.getUsuarioId(), notif);
        }
        // Resolver puntoParqueoId desde la reserva persistida (el evento no lo lleva)
        Long puntoId = null;
        if (event.getReservaId() != null) {
            Reserva r = reservaRepository.findById(event.getReservaId()).orElse(null);
            if (r != null && r.getPuntoParqueo() != null) {
                puntoId = r.getPuntoParqueo().getId();
            }
        }
        if (event.getParqueaderoId() != null) {
            if (puntoId != null) {
                emitirSpotStatusChange(puntoId, "reserved", null, event.getParqueaderoId());
            }
            emitirOcupacionActualizada(event.getParqueaderoId());
        }
    }

    @Async
    @EventListener
    public void onReservaCancelada(ReservaCanceladaEvent event) {
        if (event.getParqueaderoId() == null) return;
        if (event.getPuntoParqueoId() != null) {
            emitirSpotStatusChange(event.getPuntoParqueoId(), "free", null, event.getParqueaderoId());
        }
        emitirOcupacionActualizada(event.getParqueaderoId());
    }

    @Async
    @EventListener
    public void onPlacaDetectada(PlacaDetectadaEvent event) {
        if (event.getParqueaderoId() == null || event.getPlaca() == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("cameraId", event.getCamaraId() != null ? event.getCamaraId().toString() : null);
        data.put("tipoCamara", event.getTipoCamara());
        data.put("placa", event.getPlaca());
        data.put("confianza", event.getConfianza());
        data.put("detectedAt", event.getDetectedAt() != null ? event.getDetectedAt().toString() : null);

        NotificacionDTO notif = NotificacionDTO.builder()
                .tipo("PLACA_DETECTADA")
                .mensaje("Placa detectada por camara")
                .parqueaderoId(event.getParqueaderoId())
                .data(data)
                .build();
        notificationService.notificarParqueadero(event.getParqueaderoId(), notif);
    }

    @Async
    @EventListener
    public void onCamaraImagenActualizada(CamaraImagenActualizadaEvent event) {
        if (event.getParqueaderoId() == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("cameraId", event.getCamaraId().toString());
        data.put("imagenUrl", "/api/camaras/" + event.getCamaraId() + "/imagen?t=" + event.getImagenTimestamp());
        data.put("timestamp", event.getImagenTimestamp().toString());

        NotificacionDTO notif = NotificacionDTO.builder()
                .tipo("CAMERA_IMAGE_UPDATED")
                .parqueaderoId(event.getParqueaderoId())
                .data(data)
                .build();
        notificationService.notificarParqueadero(event.getParqueaderoId(), notif);
    }

    /**
     * Emite cambio de estado de un punto individual en /topic/parqueadero/{id}.
     * Payload:
     * {
     *   "tipo": "SPOT_STATUS_CHANGE",
     *   "parqueaderoId": 2,
     *   "data": {
     *     "puntoParqueoId": "5",
     *     "subseccionId": "3",
     *     "estado": "occupied",
     *     "ticketId": 42
     *   }
     * }
     */
    private void emitirSpotStatusChange(Long puntoParqueoId, String estado,
                                         Long ticketId, Long parqueaderoId) {
        if (puntoParqueoId == null || parqueaderoId == null) return;
        try {
            PuntoParqueo punto = puntoParqueoRepository.findById(puntoParqueoId).orElse(null);
            Long subseccionId = punto != null && punto.getSubSeccion() != null
                    ? punto.getSubSeccion().getId() : null;

            Map<String, Object> data = new HashMap<>();
            data.put("puntoParqueoId", puntoParqueoId.toString());
            if (subseccionId != null) data.put("subseccionId", subseccionId.toString());
            data.put("estado", estado);
            if (ticketId != null) data.put("ticketId", ticketId);

            NotificacionDTO notif = NotificacionDTO.builder()
                    .tipo("SPOT_STATUS_CHANGE")
                    .parqueaderoId(parqueaderoId)
                    .data(data)
                    .build();
            notificationService.notificarParqueadero(parqueaderoId, notif);
        } catch (Exception e) {
            log.warn("No se pudo emitir SPOT_STATUS_CHANGE para punto {}: {}",
                    puntoParqueoId, e.getMessage());
        }
    }

    private void emitirOcupacionActualizada(Long parqueaderoId) {
        try {
            DisponibilidadDTO disp = disponibilidadService.calcular(parqueaderoId);
            NotificacionDTO notif = NotificacionDTO.builder()
                    .tipo("OCUPACION_ACTUALIZADA")
                    .mensaje("Disponibilidad del parqueadero actualizada")
                    .parqueaderoId(parqueaderoId)
                    .data(disp)
                    .build();
            notificationService.notificarParqueadero(parqueaderoId, notif);
        } catch (Exception e) {
            log.warn("No se pudo emitir OCUPACION_ACTUALIZADA para parqueadero {}: {}",
                    parqueaderoId, e.getMessage());
        }
    }
}
