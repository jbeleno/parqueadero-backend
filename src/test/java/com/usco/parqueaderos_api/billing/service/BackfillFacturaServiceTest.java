package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.listener.AutoFacturaListener;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackfillFacturaServiceTest {

    private TicketRepository ticketRepo;
    private FacturaRepository facturaRepo;
    private AutoFacturaListener listener;
    private CurrentUserService user;
    private BackfillFacturaService service;

    @BeforeEach
    void setup() {
        ticketRepo = Mockito.mock(TicketRepository.class);
        facturaRepo = Mockito.mock(FacturaRepository.class);
        listener = Mockito.mock(AutoFacturaListener.class);
        user = Mockito.mock(CurrentUserService.class);
        service = new BackfillFacturaService(ticketRepo, facturaRepo, listener, user);
    }

    private Ticket ticket(Long id, double monto) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setEstado("CERRADO");
        t.setMontoCalculado(monto);
        t.setFechaHoraEntrada(LocalDateTime.of(2026, 5, 26, 14, 0));
        t.setFechaHoraSalida(LocalDateTime.of(2026, 5, 26, 14, 30));
        Parqueadero p = new Parqueadero(); p.setId(7L); t.setParqueadero(p);
        Vehiculo v = new Vehiculo(); v.setPlaca("ABC123"); t.setVehiculo(v);
        return t;
    }

    @Test
    @DisplayName("USER/ADMIN no pueden ejecutar backfill")
    void rbac() {
        when(user.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () ->
                service.ejecutar(7L, LocalDate.now(), LocalDate.now(), true));
    }

    @Test
    @DisplayName("desde > hasta -> error")
    void rango_invalido() {
        when(user.isSuperAdmin()).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.ejecutar(7L, LocalDate.of(2026,5,10), LocalDate.of(2026,5,1), true));
        assertEquals("ERR_INVALID_RANGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("ventana > 90 dias -> error")
    void ventana_excesiva() {
        when(user.isSuperAdmin()).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.ejecutar(7L, LocalDate.of(2026,1,1), LocalDate.of(2026,5,1), true));
        assertEquals("ERR_INVALID_RANGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("dryRun NO crea facturas, devuelve los candidatos")
    void dry_run() {
        when(user.isSuperAdmin()).thenReturn(true);
        when(ticketRepo.findCerradosFacturables(any(), any(), any()))
                .thenReturn(List.of(ticket(1L, 5000), ticket(2L, 3000)));
        when(facturaRepo.findByTicketId(anyLong())).thenReturn(List.of());

        BackfillFacturaService.BackfillResult res = service.ejecutar(
                7L, LocalDate.of(2026,5,26), LocalDate.of(2026,5,26), true);

        assertTrue(res.dryRun());
        assertEquals(2, res.candidatos());
        assertEquals(0, res.creadas());
        verify(listener, never()).generarFacturaSiCorresponde(any(), any(), any());
    }

    @Test
    @DisplayName("apply: invoca listener para cada candidato sin factura")
    void apply_invoca_listener() {
        when(user.isSuperAdmin()).thenReturn(true);
        Ticket t1 = ticket(1L, 5000);
        when(ticketRepo.findCerradosFacturables(any(), any(), any())).thenReturn(List.of(t1));
        when(facturaRepo.findByTicketId(anyLong())).thenReturn(List.of());
        Factura fakeF = new Factura(); fakeF.setId(99L);
        when(listener.generarFacturaSiCorresponde(any(), any(), any())).thenReturn(fakeF);

        BackfillFacturaService.BackfillResult res = service.ejecutar(
                7L, LocalDate.of(2026,5,26), LocalDate.of(2026,5,26), false);

        assertFalse(res.dryRun());
        assertEquals(1, res.candidatos());
        assertEquals(1, res.creadas());
        assertEquals(0, res.omitidos());
        assertTrue(res.origenAplicado().startsWith("BACKFILL_"));
        verify(listener).generarFacturaSiCorresponde(any(), any(), any());
    }

    @Test
    @DisplayName("Candidatos que YA tienen factura vigente se excluyen del recuento")
    void filtra_ya_facturados() {
        when(user.isSuperAdmin()).thenReturn(true);
        Ticket conFactura = ticket(1L, 5000);
        Ticket sinFactura = ticket(2L, 3000);
        when(ticketRepo.findCerradosFacturables(any(), any(), any()))
                .thenReturn(List.of(conFactura, sinFactura));
        Factura existente = new Factura(); existente.setEstado("PENDIENTE");
        when(facturaRepo.findByTicketId(1L)).thenReturn(List.of(existente));
        when(facturaRepo.findByTicketId(2L)).thenReturn(List.of());

        BackfillFacturaService.BackfillResult res = service.ejecutar(
                7L, LocalDate.of(2026,5,26), LocalDate.of(2026,5,26), true);
        assertEquals(1, res.candidatos(), "Solo el que no tiene factura vigente debe contar");
    }

    @Test
    @DisplayName("Factura ANULADA no cuenta como factura vigente (permite re-facturar)")
    void factura_anulada_no_bloquea() {
        when(user.isSuperAdmin()).thenReturn(true);
        Ticket t = ticket(1L, 5000);
        when(ticketRepo.findCerradosFacturables(any(), any(), any())).thenReturn(List.of(t));
        Factura anulada = new Factura(); anulada.setEstado("ANULADA");
        when(facturaRepo.findByTicketId(1L)).thenReturn(List.of(anulada));

        BackfillFacturaService.BackfillResult res = service.ejecutar(
                7L, LocalDate.of(2026,5,26), LocalDate.of(2026,5,26), true);
        assertEquals(1, res.candidatos(), "Ticket con factura ANULADA SI es candidato");
    }
}
