package com.usco.parqueaderos_api.billing.listener;

import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutoFacturaListenerTest {

    private TicketRepository ticketRepo;
    private FacturaRepository facturaRepo;
    private TarifaCalculatorService tarifaCalc;
    private AutoFacturaListener listener;

    @BeforeEach
    void setup() {
        ticketRepo = Mockito.mock(TicketRepository.class);
        facturaRepo = Mockito.mock(FacturaRepository.class);
        tarifaCalc = new TarifaCalculatorService();
        listener = new AutoFacturaListener(ticketRepo, facturaRepo, tarifaCalc);
        // save() default echo: devuelve la misma Factura con id sintetico
        when(facturaRepo.save(any())).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            if (f.getId() == null) f.setId(999L);
            return f;
        });
    }

    private Ticket ticket(Long id, String estado, Double monto, Long suscripcionId, Tarifa tarifa) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setEstado(estado);
        t.setMontoCalculado(monto);
        t.setSuscripcionId(suscripcionId);
        t.setTarifa(tarifa);
        Parqueadero p = new Parqueadero(); p.setId(7L); t.setParqueadero(p);
        Vehiculo v = new Vehiculo(); v.setId(10L); t.setVehiculo(v);
        return t;
    }

    private Tarifa tarifa(boolean aplicaIva, double pct) {
        Tarifa t = new Tarifa();
        t.setAplicaIva(aplicaIva);
        t.setIvaPorcentaje(pct);
        return t;
    }

    @Test
    @DisplayName("Ticket cerrado con monto > 0 y sin suscripcion -> crea factura PENDIENTE")
    void crea_factura() {
        Ticket t = ticket(1L, "CERRADO", 5000.0, null, tarifa(false, 0));
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(t));
        when(facturaRepo.findByTicketId(1L)).thenReturn(List.of());

        listener.onTicketCerrado(new TicketCerradoEvent(this, 1L, 7L, null));

        ArgumentCaptor<Factura> cap = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepo).save(cap.capture());
        Factura f = cap.getValue();
        assertEquals(5000.0, f.getValorTotal());
        assertEquals("PENDIENTE", f.getEstado());
        assertNull(f.getIvaMonto());
    }

    @Test
    @DisplayName("Ticket con tarifa que aplica IVA -> factura desagrega base + iva")
    void desagrega_iva() {
        Ticket t = ticket(2L, "CERRADO", 11900.0, null, tarifa(true, 19.0));
        when(ticketRepo.findById(2L)).thenReturn(Optional.of(t));
        when(facturaRepo.findByTicketId(2L)).thenReturn(List.of());

        listener.onTicketCerrado(new TicketCerradoEvent(this, 2L, 7L, null));

        ArgumentCaptor<Factura> cap = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepo).save(cap.capture());
        Factura f = cap.getValue();
        assertEquals(11900.0, f.getValorTotal(), 0.001);
        assertEquals(10000.0, f.getBaseImponible(), 0.5);
        assertEquals(1900.0, f.getIvaMonto(), 0.5);
        assertEquals(19.0, f.getIvaPorcentaje());
    }

    @Test
    @DisplayName("Ticket cubierto por suscripcion -> NO crea factura")
    void no_crea_si_suscripcion() {
        Ticket t = ticket(3L, "CERRADO", 0.0, 99L, tarifa(false, 0));
        when(ticketRepo.findById(3L)).thenReturn(Optional.of(t));
        listener.onTicketCerrado(new TicketCerradoEvent(this, 3L, 7L, null));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    @DisplayName("Monto 0 (dentro de gracia) -> NO crea factura")
    void no_crea_si_monto_cero() {
        Ticket t = ticket(4L, "CERRADO", 0.0, null, tarifa(false, 0));
        when(ticketRepo.findById(4L)).thenReturn(Optional.of(t));
        listener.onTicketCerrado(new TicketCerradoEvent(this, 4L, 7L, null));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    @DisplayName("Si ya existe factura del ticket -> idempotente, no duplica")
    void idempotencia() {
        Ticket t = ticket(5L, "CERRADO", 5000.0, null, tarifa(false, 0));
        Factura existente = new Factura(); existente.setId(99L);
        when(ticketRepo.findById(5L)).thenReturn(Optional.of(t));
        when(facturaRepo.findByTicketId(5L)).thenReturn(List.of(existente));
        listener.onTicketCerrado(new TicketCerradoEvent(this, 5L, 7L, null));
        verify(facturaRepo, never()).save(any());
    }

    @Test
    @DisplayName("Ticket no encontrado -> no-op")
    void ticket_no_existe() {
        when(ticketRepo.findById(99L)).thenReturn(Optional.empty());
        listener.onTicketCerrado(new TicketCerradoEvent(this, 99L, 7L, null));
        verify(facturaRepo, never()).save(any());
    }
}
