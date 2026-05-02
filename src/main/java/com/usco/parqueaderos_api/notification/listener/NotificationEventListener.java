package com.usco.parqueaderos_api.notification.listener;

import com.usco.parqueaderos_api.common.event.ReservaCreadaEvent;
import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.parking.dto.DisponibilidadDTO;
import com.usco.parqueaderos_api.parking.service.DisponibilidadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final DisponibilidadService disponibilidadService;

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
        emitirOcupacionActualizada(event.getParqueaderoId());
    }

    @Async
    @EventListener
    public void onReservaCreada(ReservaCreadaEvent event) {
        if (event.getUsuarioId() == null) return;
        NotificacionDTO notif = NotificacionDTO.builder()
                .tipo("RESERVA_CREADA")
                .mensaje("Tu reserva fue creada exitosamente")
                .referenciaId(event.getReservaId())
                .parqueaderoId(event.getParqueaderoId())
                .build();
        notificationService.notificarUsuario(event.getUsuarioId(), notif);
        if (event.getParqueaderoId() != null) {
            emitirOcupacionActualizada(event.getParqueaderoId());
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
