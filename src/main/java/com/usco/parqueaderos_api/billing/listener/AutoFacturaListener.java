package com.usco.parqueaderos_api.billing.listener;

import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService.BreakdownIva;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuando un ticket se cierra con monto > 0 y no fue cubierto por suscripcion,
 * crea automaticamente una Factura PENDIENTE.
 *
 * Idempotencia: si ya existe Factura para este ticket, no crea otra.
 *
 * IVA: si la tarifa aplicaba IVA, desagrega base/iva/total en la factura.
 *
 * No corre dentro de la transaccion del ticket (REQUIRES_NEW) para que un
 * fallo en la generacion de factura no rolleen el cierre del ticket. El
 * monto y estado del ticket ya estan persistidos cuando llega aqui.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoFacturaListener {

    private final TicketRepository ticketRepository;
    private final FacturaRepository facturaRepository;
    private final TarifaCalculatorService tarifaCalculator;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTicketCerrado(TicketCerradoEvent event) {
        Ticket ticket = ticketRepository.findById(event.getTicketId()).orElse(null);
        if (ticket == null) return;
        if (!"CERRADO".equals(ticket.getEstado())) return;

        Double monto = ticket.getMontoCalculado();
        if (monto == null || monto <= 0.0) {
            log.debug("AutoFactura: ticket #{} sin monto cobrable (gracia o cubierto)", ticket.getId());
            return;
        }
        if (ticket.getSuscripcionId() != null) {
            // Suscripcion MENSUAL/PASE_DIA cubrio el ticket -> no factura.
            // ABONO_PREPAGO con saldo insuficiente SI deja monto >0 y suscripcionId null.
            // El strategy AbonoPrepago setea suscripcionId solo cuando cobertura fue total.
            log.debug("AutoFactura: ticket #{} cubierto por suscripcion {}, no se crea factura",
                    ticket.getId(), ticket.getSuscripcionId());
            return;
        }

        // Idempotencia: si ya existe factura para este ticket, salimos
        List<Factura> existentes = facturaRepository.findByTicketId(ticket.getId());
        if (!existentes.isEmpty()) {
            log.debug("AutoFactura: ticket #{} ya tiene {} factura(s)", ticket.getId(), existentes.size());
            return;
        }

        Factura f = new Factura();
        f.setTicket(ticket);
        f.setParqueadero(ticket.getParqueadero());
        f.setVehiculo(ticket.getVehiculo());
        f.setFechaHora(LocalDateTime.now());
        f.setValorTotal(monto);
        f.setEstado("PENDIENTE");

        // Desagrega IVA si la tarifa lo requiere
        if (ticket.getTarifa() != null) {
            BreakdownIva b = tarifaCalculator.desagregarIva(ticket.getTarifa(), monto);
            if (b.iva() > 0.0) {
                f.setBaseImponible(b.base());
                f.setIvaMonto(b.iva());
                f.setIvaPorcentaje(ticket.getTarifa().getIvaPorcentaje());
            }
        }

        Factura saved = facturaRepository.save(f);
        log.info("AutoFactura: factura #{} creada para ticket #{} valor=${}",
                saved.getId(), ticket.getId(), monto);
    }
}
