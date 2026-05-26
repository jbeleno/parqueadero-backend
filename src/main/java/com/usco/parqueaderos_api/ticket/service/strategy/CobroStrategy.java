package com.usco.parqueaderos_api.ticket.service.strategy;

import com.usco.parqueaderos_api.ticket.entity.Ticket;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Estrategia de cobro al cerrar un ticket. Las implementaciones se ordenan
 * con @Order y CobroOrchestrator prueba cada una hasta encontrar la que aplique.
 *
 * Prelacion:
 *   1. MensualCobroStrategy
 *   2. PaseDiaCobroStrategy
 *   3. AbonoPrepagoCobroStrategy
 *   99. TarifaNormalCobroStrategy (fallback siempre aplica)
 */
public interface CobroStrategy {

    /**
     * @return Optional.empty() si esta estrategia no aplica al ticket;
     *         Optional.of(CobroResult) si aplica.
     */
    Optional<CobroResult> intentarCobrar(Ticket ticket, LocalDateTime salida);
}
