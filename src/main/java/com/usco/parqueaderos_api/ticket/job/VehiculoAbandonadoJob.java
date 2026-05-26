package com.usco.parqueaderos_api.ticket.job;

import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detecta vehiculos con ticket EN_CURSO cuya entrada es anterior al umbral
 * (default 30 dias). Emite notificacion WebSocket para que el operador
 * decida si reclamarlo, contactar al duenio, llamar a la grua, etc.
 *
 * No cierra el ticket automaticamente — la accion legal/operacional
 * depende del parqueadero.
 *
 * Corre 1 vez al dia a las 03:00 (cron 0 0 3 * * *).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehiculoAbandonadoJob {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    @Value("${app.abandono.umbral-dias:30}")
    private int umbralDias;

    @Scheduled(cron = "${app.abandono.cron:0 0 3 * * *}")
    @Transactional(readOnly = true)
    public void detectarAbandonados() {
        LocalDateTime corte = LocalDateTime.now().minusDays(umbralDias);
        List<Ticket> candidatos = ticketRepository.findEnCursoEntradaAntesDe(corte);
        if (candidatos.isEmpty()) {
            log.info("VehiculoAbandonadoJob: 0 candidatos (umbral={}d)", umbralDias);
            return;
        }
        log.warn("VehiculoAbandonadoJob: {} ticket(s) abandonados detectados (umbral={}d)",
                candidatos.size(), umbralDias);

        for (Ticket t : candidatos) {
            if (t.getParqueadero() == null) continue;
            long horas = Duration.between(t.getFechaHoraEntrada(), LocalDateTime.now()).toHours();
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", t.getId());
            data.put("placa", t.getVehiculo() != null ? t.getVehiculo().getPlaca() : null);
            data.put("entrada", t.getFechaHoraEntrada().toString());
            data.put("horasEnParqueadero", horas);
            data.put("umbralDias", umbralDias);

            NotificacionDTO n = NotificacionDTO.builder()
                    .tipo("VEHICULO_ABANDONADO")
                    .mensaje("Vehiculo " + (t.getVehiculo() != null ? t.getVehiculo().getPlaca() : "?")
                            + " lleva mas de " + umbralDias + " dias en el parqueadero")
                    .referenciaId(t.getId())
                    .parqueaderoId(t.getParqueadero().getId())
                    .data(data)
                    .build();
            notificationService.notificarParqueadero(t.getParqueadero().getId(), n);
        }
    }
}
