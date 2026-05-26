package com.usco.parqueaderos_api.subscription.service;

import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
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
 * Notifica anticipadamente al operador y al duenio del vehiculo que una
 * suscripcion (MENSUAL t&iacute;picamente) esta proxima a vencer.
 * Default: 3 dias antes.
 *
 * Corre 1 vez al dia a las 09:00. No marca la suscripcion ni la renueva,
 * solo emite alerta. El SuscripcionVencimientoJob horario se encarga del
 * cambio de estado VENCIDA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuscripcionAvisoVencimientoJob {

    private final SuscripcionRepository suscripcionRepo;
    private final NotificationService notificationService;

    @Value("${app.suscripcion.aviso-dias:3}")
    private int avisoDias;

    @Scheduled(cron = "${app.suscripcion.aviso-cron:0 0 9 * * *}")
    @Transactional(readOnly = true)
    public void notificarProximasAVencer() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusDays(avisoDias);
        List<Suscripcion> proximas = suscripcionRepo.findProximasAVencer(ahora, limite, null);
        if (proximas.isEmpty()) {
            log.info("SuscripcionAvisoVencimientoJob: 0 suscripciones por vencer en {} dias", avisoDias);
            return;
        }
        log.info("SuscripcionAvisoVencimientoJob: {} suscripciones por vencer en {} dias",
                proximas.size(), avisoDias);

        for (Suscripcion s : proximas) {
            if (s.getParqueadero() == null) continue;
            long diasRest = Duration.between(ahora, s.getFechaFin()).toDays();
            Map<String, Object> data = new HashMap<>();
            data.put("suscripcionId", s.getId());
            data.put("tipo", s.getTipo().name());
            data.put("placa", s.getVehiculo() != null ? s.getVehiculo().getPlaca() : null);
            data.put("fechaFin", s.getFechaFin().toString());
            data.put("diasRestantes", diasRest);

            NotificacionDTO n = NotificacionDTO.builder()
                    .tipo("SUSCRIPCION_PROXIMA_A_VENCER")
                    .mensaje((s.getVehiculo() != null ? s.getVehiculo().getPlaca() : "Suscripcion")
                            + " vence en " + diasRest + " dia(s)")
                    .referenciaId(s.getId())
                    .parqueaderoId(s.getParqueadero().getId())
                    .data(data)
                    .build();
            notificationService.notificarParqueadero(s.getParqueadero().getId(), n);
        }
    }
}
