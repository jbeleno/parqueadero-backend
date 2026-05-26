package com.usco.parqueaderos_api.vehicle.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehiculoServiceDeleteTest {

    private VehiculoRepository vehRepo;
    private PersonaRepository personaRepo;
    private TipoVehiculoRepository tipoRepo;
    private TicketRepository ticketRepo;
    private ReservaRepository reservaRepo;
    private FacturaRepository facturaRepo;
    private CurrentUserService user;
    private VehiculoService service;

    @BeforeEach
    void setup() {
        vehRepo = Mockito.mock(VehiculoRepository.class);
        personaRepo = Mockito.mock(PersonaRepository.class);
        tipoRepo = Mockito.mock(TipoVehiculoRepository.class);
        ticketRepo = Mockito.mock(TicketRepository.class);
        reservaRepo = Mockito.mock(ReservaRepository.class);
        facturaRepo = Mockito.mock(FacturaRepository.class);
        user = Mockito.mock(CurrentUserService.class);
        service = new VehiculoService(vehRepo, personaRepo, tipoRepo,
                ticketRepo, reservaRepo, facturaRepo, user);

        Vehiculo v = new Vehiculo();
        v.setId(28L);
        when(vehRepo.findById(28L)).thenReturn(Optional.of(v));
    }

    @Test
    @DisplayName("Sin historial -> borrado fisico exitoso")
    void sin_historial_borra() {
        when(ticketRepo.countByVehiculoId(28L)).thenReturn(0L);
        when(reservaRepo.countByVehiculoId(28L)).thenReturn(0L);
        when(facturaRepo.countByVehiculoId(28L)).thenReturn(0L);
        service.delete(28L);
        verify(vehRepo).deleteById(28L);
    }

    @Test
    @DisplayName("Con tickets -> BusinessException ERR_VEHICULO_CON_HISTORIAL")
    void con_tickets_bloquea() {
        when(ticketRepo.countByVehiculoId(28L)).thenReturn(3L);
        when(reservaRepo.countByVehiculoId(28L)).thenReturn(0L);
        when(facturaRepo.countByVehiculoId(28L)).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete(28L));
        assertEquals("ERR_VEHICULO_CON_HISTORIAL", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("3 tickets"));
        verify(vehRepo, never()).deleteById(any());
    }

    @Test
    @DisplayName("Con facturas -> bloquea aunque tickets=0")
    void con_facturas_bloquea() {
        when(ticketRepo.countByVehiculoId(28L)).thenReturn(0L);
        when(reservaRepo.countByVehiculoId(28L)).thenReturn(0L);
        when(facturaRepo.countByVehiculoId(28L)).thenReturn(2L);
        assertThrows(BusinessException.class, () -> service.delete(28L));
        verify(vehRepo, never()).deleteById(any());
    }
}
