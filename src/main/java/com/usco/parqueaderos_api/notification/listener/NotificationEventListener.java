package com.usco.parqueaderos_api.notification.listener;

import com.usco.parqueaderos_api.common.event.ReservaCreadaEvent;
import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

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
    }
}
