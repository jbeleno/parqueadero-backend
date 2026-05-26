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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Crea automaticamente una Factura PENDIENTE cuando un ticket se cierra
 * con monto > 0 y sin suscripcion cubriendolo.
 *
 * Idempotente: si ya existe factura NO-ANULADA para el ticket, no crea otra.
 * Reentrante: el metodo publico es reusable por backfills admin.
 * Race-safe: en presencia de UNIQUE INDEX en factura(ticket_id) WHERE estado!='ANULADA',
 * captura DataIntegrityViolationException si dos hilos compiten.
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
        generarFacturaSiCorresponde(ticket, "AUTO", null);
    }

    /**
     * Genera factura para el ticket si aplica. Reusable.
     *
     * @param ticket                 ticket sobre el cual evaluar
     * @param origen                 "AUTO" (listener), "MANUAL" (POST /api/facturas), "BACKFILL_<ts>"
     * @param fechaFacturaOverride   si != null se usa como fechaHora de la factura. null -> now()
     * @return factura creada o null si no aplica / ya existia
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Factura generarFacturaSiCorresponde(Ticket ticket, String origen,
                                                LocalDateTime fechaFacturaOverride) {
        if (ticket == null) return null;
        if (!"CERRADO".equals(ticket.getEstado())) return null;

        Double monto = ticket.getMontoCalculado();
        if (monto == null || monto <= 0.0) {
            log.debug("AutoFactura: ticket #{} sin monto cobrable", ticket.getId());
            return null;
        }
        if (ticket.getSuscripcionId() != null) {
            log.debug("AutoFactura: ticket #{} cubierto por suscripcion {}",
                    ticket.getId(), ticket.getSuscripcionId());
            return null;
        }

        // Idempotencia: si ya existe factura NO-ANULADA para este ticket, salimos.
        boolean yaTieneFactura = facturaRepository.findByTicketId(ticket.getId()).stream()
                .anyMatch(f -> !"ANULADA".equals(f.getEstado()));
        if (yaTieneFactura) {
            log.debug("AutoFactura: ticket #{} ya tiene factura no-anulada", ticket.getId());
            return null;
        }

        Factura f = new Factura();
        f.setTicket(ticket);
        f.setParqueadero(ticket.getParqueadero());
        f.setVehiculo(ticket.getVehiculo());
        f.setFechaHora(fechaFacturaOverride != null ? fechaFacturaOverride : LocalDateTime.now());
        f.setValorTotal(monto);
        f.setEstado("PENDIENTE");
        f.setOrigen(origen != null ? origen : "AUTO");

        if (ticket.getTarifa() != null) {
            BreakdownIva b = tarifaCalculator.desagregarIva(ticket.getTarifa(), monto);
            if (b.iva() > 0.0) {
                f.setBaseImponible(b.base());
                f.setIvaMonto(b.iva());
                f.setIvaPorcentaje(ticket.getTarifa().getIvaPorcentaje());
            }
        }

        try {
            Factura saved = facturaRepository.save(f);
            facturaRepository.flush(); // forzar deteccion del UNIQUE en caliente
            log.info("AutoFactura ({}): factura #{} creada para ticket #{} valor=${}",
                    f.getOrigen(), saved.getId(), ticket.getId(), monto);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Race condition con otro hilo (listener + backfill simultaneos, p.ej.).
            // El UNIQUE INDEX parcial protege contra duplicacion. Saltar y loguear.
            log.warn("AutoFactura: race en ticket #{} - factura ya existe ({})",
                    ticket.getId(),
                    ex.getMostSpecificCause() != null
                            ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
            return null;
        }
    }
}
