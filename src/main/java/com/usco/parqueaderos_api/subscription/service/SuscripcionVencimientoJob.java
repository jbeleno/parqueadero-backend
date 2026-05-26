package com.usco.parqueaderos_api.subscription.service;

import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job que recorre periodicamente las suscripciones ACTIVAS cuya fecha_fin
 * ya paso y las marca como VENCIDA. Corre cada hora (cron configurable).
 *
 * El indice idx_suscripcion_vence (WHERE estado='ACTIVA' ON fecha_fin) hace
 * esta consulta O(log N) sin importar el numero de suscripciones historicas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuscripcionVencimientoJob {

    private final SuscripcionRepository suscripcionRepo;

    /** Cada hora en el minuto 0. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void vencerSuscripciones() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Suscripcion> vencidas = suscripcionRepo.findActivasVencidas(ahora);
        if (vencidas.isEmpty()) return;
        for (Suscripcion s : vencidas) {
            s.setEstado(EstadoSuscripcion.VENCIDA);
            suscripcionRepo.save(s);
        }
        log.info("Job vencimientoSuscripciones: {} suscripciones marcadas VENCIDA", vencidas.size());
    }
}
