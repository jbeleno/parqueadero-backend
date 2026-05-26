package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.parking.entity.*;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests del flujo automatico de tickets via OCR.
 * Cubre los 6 escenarios y los edge cases criticos identificados en la review.
 */
@ExtendWith(MockitoExtension.class)
class TicketAutoServiceTest {

    @Mock VehiculoRepository vehiculoRepo;
    @Mock TicketRepository ticketRepo;
    @Mock PuntoParqueoRepository puntoParqueoRepo;
    @Mock ParqueaderoRepository parqueaderoRepo;
    @Mock TarifaRepository tarifaRepo;
    @Mock TipoVehiculoRepository tipoVehiculoRepo;
    @Mock PersonaRepository personaRepo;
    @Mock TarifaCalculatorService tarifaCalculator;
    @Mock CobroOrchestrator cobroOrchestrator;
    @Mock ApplicationEventPublisher publisher;

    @InjectMocks TicketAutoService service;

    Parqueadero parq;
    Vehiculo vehiculoExistente;
    PuntoParqueo puntoLibrePublico;
    Tarifa tarifa;

    @BeforeEach
    void setup() {
        parq = new Parqueadero();
        parq.setId(7L);

        vehiculoExistente = new Vehiculo();
        vehiculoExistente.setId(10L);
        vehiculoExistente.setPlaca("KJV807");
        TipoVehiculo tv = new TipoVehiculo();
        tv.setId(1L);
        tv.setNombre("Carro");
        vehiculoExistente.setTipoVehiculo(tv);

        TipoPuntoParqueo tipoPublico = new TipoPuntoParqueo();
        tipoPublico.setId(1L);
        tipoPublico.setNombre("Placas");
        puntoLibrePublico = new PuntoParqueo();
        puntoLibrePublico.setId(100L);
        puntoLibrePublico.setTipoPuntoParqueo(tipoPublico);

        tarifa = new Tarifa();
        tarifa.setId(50L);
        tarifa.setTipoVehiculo(tv);
    }

    // ───────── ENTRADA ─────────

    @Test
    @DisplayName("ENTRADA con vehiculo existente sin ticket abierto -> ENTRADA_REGISTRADA")
    void entrada_vehiculo_existente() {
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of(puntoLibrePublico));
        when(puntoParqueoRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(puntoLibrePublico));
        when(ticketRepo.existsByPuntoParqueoIdAndEstado(100L, "EN_CURSO")).thenReturn(false);
        when(tarifaRepo.findByParqueaderoId(7L)).thenReturn(List.of(tarifa));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(999L);
            return t;
        });

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ENTRADA_REGISTRADA, r.accion());
        assertEquals(999L, r.ticketId());
        assertEquals(10L, r.vehiculoId());
        assertFalse(r.vehiculoCreado());
        assertEquals(100L, r.puntoParqueoId());
        verify(publisher).publishEvent(any());
    }

    @Test
    @DisplayName("ENTRADA con vehiculo NO existente -> crea invitado + ticket")
    void entrada_vehiculo_invitado() {
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("XYZ123")).thenReturn(Optional.empty());

        Persona invitado = new Persona();
        invitado.setId(1L);
        when(personaRepo.findByNumeroDocumento("SISTEMA_INVITADO")).thenReturn(Optional.of(invitado));
        when(tipoVehiculoRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(vehiculoExistente.getTipoVehiculo()));
        when(vehiculoRepo.save(any(Vehiculo.class))).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            v.setId(20L);
            return v;
        });
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(20L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of(puntoLibrePublico));
        when(puntoParqueoRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(puntoLibrePublico));
        when(ticketRepo.existsByPuntoParqueoIdAndEstado(100L, "EN_CURSO")).thenReturn(false);
        when(tarifaRepo.findByParqueaderoId(7L)).thenReturn(List.of(tarifa));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> { Ticket t = inv.getArgument(0); t.setId(888L); return t; });

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "XYZ123");

        assertEquals(TicketAutoService.Accion.ENTRADA_REGISTRADA, r.accion());
        assertTrue(r.vehiculoCreado(), "debe marcarse como vehiculo creado");
        assertEquals(888L, r.ticketId());
    }

    @Test
    @DisplayName("ENTRADA con race condition: el INSERT viola el indice unico -> ENTRADA_DUPLICADA")
    void entrada_race_condition_indice_unico() {
        // existsBy retorna false (otro thread aun no commiteo) pero al hacer
        // save+flush, el indice unico parcial dispara DataIntegrityViolationException.
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of(puntoLibrePublico));
        when(puntoParqueoRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(puntoLibrePublico));
        when(ticketRepo.existsByPuntoParqueoIdAndEstado(100L, "EN_CURSO")).thenReturn(false);
        when(tarifaRepo.findByParqueaderoId(7L)).thenReturn(List.of(tarifa));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> { Ticket t = inv.getArgument(0); t.setId(999L); return t; });
        // El flush dispara la violacion del indice unico
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "uniq_ticket_vehiculo_parqueadero_en_curso"))
                .when(ticketRepo).flush();

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ENTRADA_DUPLICADA, r.accion());
        assertNull(r.ticketId(), "no debe quedar ticketId del save fallido");
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("ENTRADA con ticket EN_CURSO ya abierto -> ENTRADA_DUPLICADA (no crea otro ticket)")
    void entrada_duplicada() {
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(true);

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ENTRADA_DUPLICADA, r.accion());
        assertNull(r.ticketId());
        verify(ticketRepo, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("ENTRADA sin puntos publicos libres -> ERROR")
    void entrada_sin_puntos_libres() {
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of());

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ERROR, r.accion());
        assertTrue(r.mensaje().contains("No hay puntos"));
    }

    @Test
    @DisplayName("ENTRADA: punto tipo 'administrativo' NO se autoasigna")
    void entrada_evita_puntos_no_publicos() {
        TipoPuntoParqueo admin = new TipoPuntoParqueo();
        admin.setId(2L);
        admin.setNombre("administrativo");
        PuntoParqueo puntoAdmin = new PuntoParqueo();
        puntoAdmin.setId(200L);
        puntoAdmin.setTipoPuntoParqueo(admin);

        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of(puntoAdmin));

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ERROR, r.accion());
        verify(puntoParqueoRepo, never()).findByIdForUpdate(200L);
    }

    @Test
    @DisplayName("ENTRADA: race condition de punto -> pasa al siguiente disponible")
    void entrada_race_condition_punto() {
        PuntoParqueo otroPunto = new PuntoParqueo();
        otroPunto.setId(101L);
        otroPunto.setTipoPuntoParqueo(puntoLibrePublico.getTipoPuntoParqueo());

        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(10L, 7L, "EN_CURSO")).thenReturn(false);
        when(puntoParqueoRepo.findActiveByParqueaderoId(7L)).thenReturn(List.of(puntoLibrePublico, otroPunto));
        when(puntoParqueoRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(puntoLibrePublico));
        // El primero ya esta ocupado bajo el lock (otro thread se nos adelanto)
        when(ticketRepo.existsByPuntoParqueoIdAndEstado(100L, "EN_CURSO")).thenReturn(true);
        // El segundo si esta libre
        when(puntoParqueoRepo.findByIdForUpdate(101L)).thenReturn(Optional.of(otroPunto));
        when(ticketRepo.existsByPuntoParqueoIdAndEstado(101L, "EN_CURSO")).thenReturn(false);
        when(tarifaRepo.findByParqueaderoId(7L)).thenReturn(List.of(tarifa));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> { Ticket t = inv.getArgument(0); t.setId(777L); return t; });

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 7L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ENTRADA_REGISTRADA, r.accion());
        assertEquals(101L, r.puntoParqueoId(), "debe usar el segundo punto, no el ocupado");
    }

    // ───────── SALIDA ─────────

    @Test
    @DisplayName("SALIDA con ticket EN_CURSO -> SALIDA_REGISTRADA, ticket cerrado, monto calculado")
    void salida_normal() {
        Ticket ticketAbierto = new Ticket();
        ticketAbierto.setId(500L);
        ticketAbierto.setVehiculo(vehiculoExistente);
        ticketAbierto.setParqueadero(parq);
        ticketAbierto.setPuntoParqueo(puntoLibrePublico);
        ticketAbierto.setEstado("EN_CURSO");
        ticketAbierto.setFechaHoraEntrada(LocalDateTime.now().minusHours(2));

        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
                10L, 7L, "EN_CURSO")).thenReturn(Optional.of(ticketAbierto));
        when(cobroOrchestrator.cobrar(any(Ticket.class), any(LocalDateTime.class)))
                .thenReturn(new com.usco.parqueaderos_api.ticket.service.strategy.CobroResult(
                        5000.0, null, true, "Cobro con tarifa normal"));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(6L, 7L, "SALIDA", "KJV807");

        assertEquals(TicketAutoService.Accion.SALIDA_REGISTRADA, r.accion());
        assertEquals(500L, r.ticketId());
        assertEquals(5000.0, r.montoCalculado());
        assertEquals("CERRADO", ticketAbierto.getEstado());
        assertNotNull(ticketAbierto.getFechaHoraSalida());
        assertNotNull(ticketAbierto.getFechaHoraSalidaFisica());
    }

    @Test
    @DisplayName("SALIDA con ticket cerrado hace <5 min -> SALIDA_CONFIRMADA_FISICA (no recobra)")
    void salida_fisica_confirmada() {
        Ticket cerradoReciente = new Ticket();
        cerradoReciente.setId(600L);
        cerradoReciente.setEstado("CERRADO");
        cerradoReciente.setFechaHoraEntrada(LocalDateTime.now().minusHours(1));
        cerradoReciente.setFechaHoraSalida(LocalDateTime.now().minusMinutes(2)); // hace 2 min
        cerradoReciente.setMontoCalculado(3000.0);
        cerradoReciente.setParqueadero(parq);
        cerradoReciente.setPuntoParqueo(puntoLibrePublico);

        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
                10L, 7L, "EN_CURSO")).thenReturn(Optional.empty());
        when(ticketRepo.findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
                10L, 7L)).thenReturn(Optional.of(cerradoReciente));

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(6L, 7L, "SALIDA", "KJV807");

        assertEquals(TicketAutoService.Accion.SALIDA_CONFIRMADA_FISICA, r.accion());
        assertEquals(600L, r.ticketId());
        assertNotNull(cerradoReciente.getFechaHoraSalidaFisica(), "debe registrar fechaHoraSalidaFisica");
        verify(tarifaCalculator, never()).calcular(any(), any()); // no debe recobrar
    }

    @Test
    @DisplayName("SALIDA con ticket cerrado hace >5 min -> SALIDA_SIN_TICKET (alerta)")
    void salida_fantasma_por_tiempo() {
        Ticket cerradoViejo = new Ticket();
        cerradoViejo.setId(601L);
        cerradoViejo.setEstado("CERRADO");
        cerradoViejo.setFechaHoraEntrada(LocalDateTime.now().minusHours(2));
        cerradoViejo.setFechaHoraSalida(LocalDateTime.now().minusMinutes(30)); // hace 30 min
        cerradoViejo.setParqueadero(parq);

        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("KJV807")).thenReturn(Optional.of(vehiculoExistente));
        when(ticketRepo.findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
                10L, 7L, "EN_CURSO")).thenReturn(Optional.empty());
        when(ticketRepo.findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
                10L, 7L)).thenReturn(Optional.of(cerradoViejo));

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(6L, 7L, "SALIDA", "KJV807");

        assertEquals(TicketAutoService.Accion.SALIDA_SIN_TICKET, r.accion());
    }

    @Test
    @DisplayName("SALIDA con vehiculo no registrado -> SALIDA_SIN_TICKET")
    void salida_vehiculo_desconocido() {
        when(parqueaderoRepo.findById(7L)).thenReturn(Optional.of(parq));
        when(vehiculoRepo.findByPlaca("XYZ999")).thenReturn(Optional.empty());

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(6L, 7L, "SALIDA", "XYZ999");

        assertEquals(TicketAutoService.Accion.SALIDA_SIN_TICKET, r.accion());
        verify(ticketRepo, never()).save(any());
    }

    // ───────── SEGURIDAD / ERROR ─────────

    @Test
    @DisplayName("Camara SEGURIDAD -> SOLO_DETECCION (no toca BD)")
    void camara_seguridad() {
        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(8L, 7L, "SEGURIDAD", "KJV807");

        assertEquals(TicketAutoService.Accion.SOLO_DETECCION, r.accion());
        verifyNoInteractions(parqueaderoRepo, vehiculoRepo, ticketRepo);
    }

    @Test
    @DisplayName("Parqueadero no existe -> ERROR (no crashea)")
    void parqueadero_no_existe() {
        when(parqueaderoRepo.findById(999L)).thenReturn(Optional.empty());

        TicketAutoService.AutoActionResult r = service.procesarPlacaDetectada(5L, 999L, "ENTRADA", "KJV807");

        assertEquals(TicketAutoService.Accion.ERROR, r.accion());
        verify(ticketRepo, never()).save(any());
    }
}
