package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.convenio.entity.ValidacionCompra;
import com.usco.parqueaderos_api.convenio.repository.ValidacionCompraRepository;
import com.usco.parqueaderos_api.convenio.service.ConvenioDescuentoCalculator;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.service.strategy.CobroResult;
import com.usco.parqueaderos_api.ticket.service.strategy.CobroStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final ValidacionCompraRepository validacionRepository;
    private final ConvenioDescuentoCalculator descuentoCalculator;

    public CobroResult cobrar(Ticket ticket, LocalDateTime salida) {
        for (CobroStrategy s : strategies) {
            var r = s.intentarCobrar(ticket, salida);
            if (r.isPresent()) {
                CobroResult base = r.get();
                CobroResult conDescuento = aplicarConvenio(ticket, salida, base);
                log.debug("CobroOrchestrator ticket #{}: strategy={} monto_base={} monto_final={} susc={}",
                        ticket.getId(), s.getClass().getSimpleName(),
                        base.montoCobrado(), conDescuento.montoCobrado(), conDescuento.suscripcionId());
                return conDescuento;
            }
        }
        // No deberia llegar aqui porque TarifaNormalCobroStrategy siempre aplica
        throw new IllegalStateException("Ninguna estrategia de cobro aplico al ticket " + ticket.getId());
    }

    /**
     * Si el ticket tiene una ValidacionCompra registrada, aplica el descuento del
     * convenio sobre el monto cobrado. No toca suscripciones: el descuento solo
     * tiene sentido cuando hay cobro real (TarifaNormalCobroStrategy).
     */
    private CobroResult aplicarConvenio(Ticket ticket, LocalDateTime salida, CobroResult base) {
        if (base.montoCobrado() <= 0) return base; // nada que descontar
        if (ticket.getId() == null) return base;
        Optional<ValidacionCompra> v = validacionRepository
                .findFirstByTicketIdOrderByFechaAplicacionDesc(ticket.getId());
        if (v.isEmpty()) return base;
        ValidacionCompra vc = v.get();
        long minutos = Math.max(0,
                Duration.between(ticket.getFechaHoraEntrada(), salida).toMinutes());
        double descuento = descuentoCalculator.aplicar(
                vc.getConvenio(), base.montoCobrado(), vc.getMontoCompra(), minutos);
        if (descuento <= 0) return base;
        double nuevoMonto = Math.max(0.0, base.montoCobrado() - descuento);
        vc.setDescuentoAplicado(descuento);
        validacionRepository.save(vc);
        return new CobroResult(
                nuevoMonto,
                base.suscripcionId(),
                nuevoMonto > 0,
                base.mensaje() + " (descuento convenio: -" + descuento + ")"
        );
    }
}
