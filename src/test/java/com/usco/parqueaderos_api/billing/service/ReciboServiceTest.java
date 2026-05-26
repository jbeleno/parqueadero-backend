package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ReciboServiceTest {

    private FacturaRepository facturaRepo;
    private PagoRepository pagoRepo;
    private CurrentUserService user;
    private ReciboService service;

    @BeforeEach
    void setup() {
        facturaRepo = Mockito.mock(FacturaRepository.class);
        pagoRepo = Mockito.mock(PagoRepository.class);
        user = Mockito.mock(CurrentUserService.class);
        when(user.isSuperAdmin()).thenReturn(true);
        service = new ReciboService(facturaRepo, pagoRepo, user);
    }

    @Test
    @DisplayName("Recibo TXT incluye placa, total, IVA desagregado y pagos")
    void recibo_completo() {
        Empresa e = new Empresa(); e.setId(2L); e.setNombre("USCO Parqueaderos"); e.setNit("900111222-3");
        Parqueadero p = new Parqueadero(); p.setId(7L); p.setNombre("Sede Norte"); p.setEmpresa(e);
        Vehiculo v = new Vehiculo(); v.setPlaca("ABC123");
        Ticket t = new Ticket();
        t.setId(42L);
        t.setVehiculo(v);
        t.setFechaHoraEntrada(LocalDateTime.of(2026, 5, 1, 10, 0));
        t.setFechaHoraSalida(LocalDateTime.of(2026, 5, 1, 11, 30));
        Factura f = new Factura();
        f.setId(99L);
        f.setTicket(t);
        f.setParqueadero(p);
        f.setVehiculo(v);
        f.setValorTotal(11900.0);
        f.setBaseImponible(10000.0);
        f.setIvaMonto(1900.0);
        f.setIvaPorcentaje(19.0);
        f.setEstado("PAGADA");
        f.setFechaHora(LocalDateTime.of(2026, 5, 1, 11, 30));
        when(facturaRepo.findById(99L)).thenReturn(Optional.of(f));

        Pago pago = new Pago();
        pago.setId(1L); pago.setMonto(11900.0); pago.setMetodo("EFECTIVO");
        pago.setFechaHora(LocalDateTime.of(2026, 5, 1, 11, 30));
        pago.setEstado("COMPLETADO");
        when(pagoRepo.findByFacturaId(99L)).thenReturn(List.of(pago));

        String txt = service.generarTxt(99L);
        assertTrue(txt.contains("ABC123"), "incluye placa");
        assertTrue(txt.contains("USCO Parqueaderos"), "incluye empresa");
        assertTrue(txt.contains("900111222-3"), "incluye NIT");
        assertTrue(txt.contains("Sede Norte"), "incluye parqueadero");
        assertTrue(txt.contains("Factura #99"), "incluye id factura");
        assertTrue(txt.contains("Duracion"), "incluye duracion");
        assertTrue(txt.contains("IVA 19%"), "incluye IVA");
        assertTrue(txt.contains("$10,000"), "base imponible formateada");
        assertTrue(txt.contains("$1,900"), "iva formateado");
        assertTrue(txt.contains("$11,900"), "total formateado");
        assertTrue(txt.contains("EFECTIVO"), "incluye metodo de pago");
    }
}
