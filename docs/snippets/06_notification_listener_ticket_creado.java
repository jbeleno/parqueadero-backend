// NotificationEventListener.java — listener async que dispara los WS (Figura 18)
@Async
@EventListener
public void onTicketCreado(TicketCreadoEvent event) {
    if (event.getParqueaderoId() == null) return;

    // 1) TICKET_CREADO al tópico del parqueadero
    NotificacionDTO notif = NotificacionDTO.builder()
            .tipo("TICKET_CREADO")
            .mensaje("Nuevo ticket creado en el parqueadero")
            .referenciaId(event.getTicketId())
            .parqueaderoId(event.getParqueaderoId())
            .build();
    notificationService.notificarParqueadero(event.getParqueaderoId(), notif);

    // 2) SPOT_STATUS_CHANGE con estado occupied
    emitirSpotStatusChange(event.getPuntoParqueoId(), "occupied",
            event.getTicketId(), event.getParqueaderoId());

    // 3) OCUPACION_ACTUALIZADA con los nuevos contadores
    emitirOcupacionActualizada(event.getParqueaderoId());
}
