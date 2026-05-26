package com.usco.parqueaderos_api.ticket.service.strategy;

import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Fallback: si ninguna otra estrategia aplico, cobrar con la tarifa normal
 * (que incluye Modelo B: gracia + valor minimo + tarifa por excedente).
 *
 * Siempre devuelve un CobroResult (nunca empty).
 */
@Component
@Order(99)
@RequiredArgsConstructor
public class TarifaNormalCobroStrategy implements CobroStrategy {

    private final TarifaCalculatorService tarifaCalculator;

    @Override
    public Optional<CobroResult> intentarCobrar(Ticket ticket, LocalDateTime salida) {
        double monto = tarifaCalculator.calcular(ticket, salida);
        return Optional.of(new CobroResult(
                monto,
                null,                  // sin suscripcion
                monto > 0,             // factura pendiente si hay monto a cobrar
                monto == 0
                        ? "Dentro de minutos de gracia"
                        : "Cobro con tarifa normal"
        ));
    }
}
