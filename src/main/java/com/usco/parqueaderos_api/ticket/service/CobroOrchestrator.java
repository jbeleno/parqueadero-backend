package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.service.strategy.CobroResult;
import com.usco.parqueaderos_api.ticket.service.strategy.CobroStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquesta las estrategias de cobro al cerrar un ticket.
 *
 * Spring inyecta la lista de CobroStrategy ya ordenadas por @Order. La primera
 * que retorne Optional.present() es la que aplica. TarifaNormalCobroStrategy
 * (@Order 99) siempre aplica como fallback, asi que el metodo cobrar nunca falla.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CobroOrchestrator {

    private final List<CobroStrategy> strategies;

    public CobroResult cobrar(Ticket ticket, LocalDateTime salida) {
        for (CobroStrategy s : strategies) {
            var r = s.intentarCobrar(ticket, salida);
            if (r.isPresent()) {
                log.debug("CobroOrchestrator ticket #{}: strategy={} monto={} susc={}",
                        ticket.getId(), s.getClass().getSimpleName(),
                        r.get().montoCobrado(), r.get().suscripcionId());
                return r.get();
            }
        }
        // No deberia llegar aqui porque TarifaNormalCobroStrategy siempre aplica
        throw new IllegalStateException("Ninguna estrategia de cobro aplico al ticket " + ticket.getId());
    }
}
