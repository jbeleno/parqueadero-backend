package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.dto.TicketDTO;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests del TicketService blindado:
 * - Inyeccion de monto rechazada (cliente envia montoCalculado=0,
 *   backend lo ignora y calcula auto)
 * - Race condition: dos saves al mismo punto, el segundo lanza
 *   ERR_POINT_OCCUPIED
 * - RBAC: USER no puede crear ni cerrar tickets
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock ParqueaderoRepository parqueaderoRepository;
    @Mock PuntoParqueoRepository puntoParqueoRepository;
    @Mock VehiculoRepository vehiculoRepository;
    @Mock TarifaRepository tarifaRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock CurrentUserService currentUser;
    @Mock TarifaCalculatorService tarifaCalculator;

    @InjectMocks TicketService ticketService;

    private Empresa empresa;
    private Parqueadero parqueadero;
    private PuntoParqueo punto;
    private Vehiculo vehiculo;
    private Tarifa tarifa;

    @BeforeEach
    void setUp() {
        empresa = new Empresa(); empresa.setId(1L); empresa.setNombre("USCO");
        parqueadero = new Parqueadero(); parqueadero.setId(2L); parqueadero.setEmpresa(empresa);
        punto = new PuntoParqueo(); punto.setId(5L);
        vehiculo = new Vehiculo(); vehiculo.setId(10L); vehiculo.setPlaca("ABC123");
        tarifa = new Tarifa(); tarifa.setId(20L); tarifa.setUnidad("POR_HORA"); tarifa.setValor(3000.0);
    }

    private TicketDTO dtoCompleto() {
        TicketDTO dto = new TicketDTO();
        dto.setParqueaderoId(parqueadero.getId());
        dto.setPuntoParqueoId(punto.getId());
        dto.setVehiculoId(vehiculo.getId());
        dto.setTarifaId(tarifa.getId());
        return dto;
    }

    @Test
    @DisplayName("save: USER recibe AccessDenied (solo ADMIN/SUPER_ADMIN crean tickets)")
    void save_userNoPuedeCrear_403() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> ticketService.save(dtoCompleto()));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("save: race condition - segundo intento al mismo punto lanza ERR_POINT_OCCUPIED")
    void save_puntoYaOcupado_lanzaConflict() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(parqueaderoRepository.findById(parqueadero.getId())).thenReturn(Optional.of(parqueadero));
        // No se llamara requireEmpresa porque mock por defecto no lanza
        when(puntoParqueoRepository.findByIdForUpdate(punto.getId())).thenReturn(Optional.of(punto));
        // Hay un ticket EN_CURSO en ese punto
        when(ticketRepository.existsByPuntoParqueoIdAndEstado(punto.getId(), "EN_CURSO")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.save(dtoCompleto()));
        assertEquals("ERR_POINT_OCCUPIED", ex.getErrorCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("save: ignora montoCalculado del DTO y setea fechaHoraEntrada del servidor")
    void save_ignoraMontoYFechaDelDto() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(parqueaderoRepository.findById(parqueadero.getId())).thenReturn(Optional.of(parqueadero));
        when(puntoParqueoRepository.findByIdForUpdate(punto.getId())).thenReturn(Optional.of(punto));
        when(ticketRepository.existsByPuntoParqueoIdAndEstado(punto.getId(), "EN_CURSO")).thenReturn(false);
        when(vehiculoRepository.findById(vehiculo.getId())).thenReturn(Optional.of(vehiculo));
        when(tarifaRepository.findById(tarifa.getId())).thenReturn(Optional.of(tarifa));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TicketDTO dto = dtoCompleto();
        // Cliente intenta inyectar:
        dto.setMontoCalculado(99999.0);
        dto.setEstado("CERRADO"); // intento de saltarse el flujo
        dto.setFechaHoraEntrada(LocalDateTime.of(1970, 1, 1, 0, 0)); // fecha inyectada

        ticketService.save(dto);

        verify(ticketRepository).save(argThat(saved ->
                saved.getMontoCalculado() == null
                && "EN_CURSO".equals(saved.getEstado())
                && saved.getFechaHoraEntrada() != null
                && saved.getFechaHoraEntrada().getYear() >= 2024
        ));
    }

    @Test
    @DisplayName("update: cerrar ticket calcula monto AUTO, ignora montoCalculado del DTO")
    void update_cierraTicket_montoCalculadoAuto() {
        Ticket existente = new Ticket();
        existente.setId(50L);
        existente.setEstado("EN_CURSO");
        existente.setFechaHoraEntrada(LocalDateTime.now().minusHours(2));
        existente.setParqueadero(parqueadero);
        existente.setPuntoParqueo(punto);
        existente.setVehiculo(vehiculo);
        existente.setTarifa(tarifa);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(existente));
        when(tarifaCalculator.calcular(eq(existente), any(LocalDateTime.class))).thenReturn(6000.0);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketDTO dto = new TicketDTO();
        dto.setEstado("CERRADO");
        dto.setMontoCalculado(0.0); // intento de fraude

        ticketService.update(50L, dto);

        // El monto persistido debe ser el calculado por el service, no el del DTO
        verify(ticketRepository).save(argThat(saved ->
                "CERRADO".equals(saved.getEstado())
                && saved.getMontoCalculado() != null
                && saved.getMontoCalculado() == 6000.0
                && saved.getFechaHoraSalida() != null
        ));
        verify(tarifaCalculator).calcular(eq(existente), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("update: USER no puede cerrar tickets")
    void update_userNoPuedeCerrar_403() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.isSuperAdmin()).thenReturn(false);

        TicketDTO dto = new TicketDTO();
        dto.setEstado("CERRADO");

        assertThrows(AccessDeniedException.class,
                () -> ticketService.update(50L, dto));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: transicion CERRADO -> CERRADO es no-op")
    void update_estadoMismo_noOp() {
        Ticket existente = new Ticket();
        existente.setId(50L);
        existente.setEstado("CERRADO");
        existente.setMontoCalculado(6000.0);
        existente.setParqueadero(parqueadero);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(existente));

        TicketDTO dto = new TicketDTO();
        dto.setEstado("CERRADO");

        ticketService.update(50L, dto);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: no se puede modificar un ticket ya CERRADO")
    void update_estadoYaCerrado_lanza() {
        Ticket existente = new Ticket();
        existente.setId(50L);
        existente.setEstado("CERRADO");
        existente.setParqueadero(parqueadero);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(existente));

        TicketDTO dto = new TicketDTO();
        dto.setEstado("EN_CURSO");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.update(50L, dto));
        assertEquals("ERR_INVALID_TRANSITION", ex.getErrorCode());
    }

    // ───────────────── cambiarPunto ─────────────────

    private Ticket ticketEnCursoConPunto() {
        Ticket t = new Ticket();
        t.setId(60L);
        t.setEstado("EN_CURSO");
        t.setParqueadero(parqueadero);
        t.setPuntoParqueo(punto);
        return t;
    }

    private PuntoParqueo puntoEnMismoParqueadero(Long id) {
        PuntoParqueo destino = new PuntoParqueo();
        destino.setId(id);
        // estructura subSeccion -> seccion -> parqueadero requerida por validacion
        com.usco.parqueaderos_api.parking.entity.SubSeccion ss = new com.usco.parqueaderos_api.parking.entity.SubSeccion();
        com.usco.parqueaderos_api.parking.entity.Seccion sec = new com.usco.parqueaderos_api.parking.entity.Seccion();
        sec.setParqueadero(parqueadero);
        ss.setSeccion(sec);
        destino.setSubSeccion(ss);
        return destino;
    }

    @Test
    @DisplayName("cambiarPunto: USER recibe AccessDenied")
    void cambiarPunto_user_403() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> ticketService.cambiarPunto(60L, 99L));
    }

    @Test
    @DisplayName("cambiarPunto: ticket CERRADO no se puede mover")
    void cambiarPunto_ticketCerrado_lanza() {
        Ticket cerrado = new Ticket();
        cerrado.setId(60L); cerrado.setEstado("CERRADO");
        cerrado.setParqueadero(parqueadero); cerrado.setPuntoParqueo(punto);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(60L)).thenReturn(Optional.of(cerrado));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.cambiarPunto(60L, 99L));
        assertEquals("ERR_INVALID_TRANSITION", ex.getErrorCode());
    }

    @Test
    @DisplayName("cambiarPunto: punto destino ocupado por otro ticket -> ERR_POINT_OCCUPIED")
    void cambiarPunto_destinoOcupado_lanza() {
        Ticket abierto = ticketEnCursoConPunto();
        PuntoParqueo destino = puntoEnMismoParqueadero(99L);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(60L)).thenReturn(Optional.of(abierto));
        when(puntoParqueoRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(destino));
        when(ticketRepository.existsByPuntoParqueoIdAndEstado(99L, "EN_CURSO")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.cambiarPunto(60L, 99L));
        assertEquals("ERR_POINT_OCCUPIED", ex.getErrorCode());
    }

    @Test
    @DisplayName("cambiarPunto: punto destino de OTRO parqueadero -> ERR_INVALID_STATE")
    void cambiarPunto_destinoOtroParqueadero_lanza() {
        Ticket abierto = ticketEnCursoConPunto();
        // Destino con parqueadero distinto
        PuntoParqueo destino = new PuntoParqueo();
        destino.setId(99L);
        com.usco.parqueaderos_api.parking.entity.SubSeccion ss = new com.usco.parqueaderos_api.parking.entity.SubSeccion();
        com.usco.parqueaderos_api.parking.entity.Seccion sec = new com.usco.parqueaderos_api.parking.entity.Seccion();
        Parqueadero otroParq = new Parqueadero(); otroParq.setId(99L);
        sec.setParqueadero(otroParq);
        ss.setSeccion(sec);
        destino.setSubSeccion(ss);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(60L)).thenReturn(Optional.of(abierto));
        when(puntoParqueoRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(destino));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketService.cambiarPunto(60L, 99L));
        assertEquals("ERR_INVALID_STATE", ex.getErrorCode());
    }

    @Test
    @DisplayName("cambiarPunto: mismo punto = no-op (sin cambios)")
    void cambiarPunto_mismoPunto_noop() {
        Ticket abierto = ticketEnCursoConPunto();
        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(60L)).thenReturn(Optional.of(abierto));

        TicketDTO res = ticketService.cambiarPunto(60L, 5L); // mismo id que punto actual

        assertEquals(5L, res.getPuntoParqueoId());
        verify(ticketRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("cambiarPunto: caso feliz -> save + publishEvent(TicketPuntoCambiadoEvent)")
    void cambiarPunto_ok() {
        Ticket abierto = ticketEnCursoConPunto();
        PuntoParqueo destino = puntoEnMismoParqueadero(99L);

        when(currentUser.isAdmin()).thenReturn(true);
        when(ticketRepository.findById(60L)).thenReturn(Optional.of(abierto));
        when(puntoParqueoRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(destino));
        when(ticketRepository.existsByPuntoParqueoIdAndEstado(99L, "EN_CURSO")).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketDTO res = ticketService.cambiarPunto(60L, 99L);

        assertEquals(99L, res.getPuntoParqueoId());
        verify(ticketRepository).save(abierto);
        verify(eventPublisher).publishEvent(any());
    }
}
