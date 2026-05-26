package com.usco.parqueaderos_api.ticket.service.strategy;

import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import com.usco.parqueaderos_api.subscription.service.SaldoService;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Si el vehiculo tiene Suscripcion ABONO_PREPAGO activa, calcula la tarifa
 * normal y descuenta del saldo. Si el saldo no alcanza, descuenta lo que
 * pueda y deja factura pendiente por la diferencia.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class AbonoPrepagoCobroStrategy implements CobroStrategy {

    private final SuscripcionRepository suscripcionRepo;
    private final SaldoService saldoService;
    private final TarifaCalculatorService tarifaCalculator;

    @Override
    public Optional<CobroResult> intentarCobrar(Ticket ticket, LocalDateTime salida) {
        if (ticket.getVehiculo() == null || ticket.getParqueadero() == null) return Optional.empty();
        Optional<Suscripcion> abono = suscripcionRepo
                .findFirstByVehiculoIdAndParqueaderoIdAndTipoAndEstado(
                        ticket.getVehiculo().getId(),
                        ticket.getParqueadero().getId(),
                        TipoSuscripcion.ABONO_PREPAGO,
                        EstadoSuscripcion.ACTIVA);
        if (abono.isEmpty()) return Optional.empty();
        Suscripcion s = abono.get();

        // Calcular monto normal con tarifa (incluye gracia + minimo del Modelo B)
        double montoNormal = tarifaCalculator.calcular(ticket, salida);
        if (montoNormal <= 0) {
            // Cubierto por gracia, no se descuenta nada
            return Optional.of(new CobroResult(0.0, s.getId(), false,
                    "Dentro de minutos de gracia, no se descuenta saldo"));
        }

        // Descontar lo que se pueda del saldo
        SaldoService.DescuentoResult d = saldoService.descontar(s.getId(), montoNormal, ticket);

        if (d.pendiente() <= 0.0001) {
            // Cubierto completamente por saldo
            return Optional.of(new CobroResult(
                    d.descontado(),
                    s.getId(),
                    false, // ya pagado con saldo
                    "Descontado del saldo prepago (saldo restante: " + (s.getSaldoRestante() - d.descontado()) + ")"
            ));
        }

        // Saldo insuficiente: queda pendiente la diferencia
        return Optional.of(new CobroResult(
                montoNormal,             // monto total cobrado al ticket
                s.getId(),
                true,                    // crear factura por el pendiente
                String.format("Saldo prepago insuficiente: descontado $%.2f, pendiente $%.2f",
                        d.descontado(), d.pendiente())
        ));
    }
}
