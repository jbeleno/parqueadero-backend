package com.usco.parqueaderos_api.ocr;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Aplica la logica de negocio del parqueadero cuando el OCR detecta una placa:
 *  - ENTRADA: si el vehiculo no existe lo crea como invitado, asigna primer punto
 *    libre y crea ticket EN_CURSO. Si ya hay ticket abierto, ignora.
 *  - SALIDA: busca ticket EN_CURSO del vehiculo en este parqueadero y lo cierra
 *    (fechaHoraSalida + montoCalculado). Si no hay, retorna ALERTA.
 *  - SEGURIDAD: no toca BD.
 *
 * Es invocado por OcrEventListener (no por un controller). Se ejecuta dentro
 * del listener async, no requiere contexto de usuario (omite el RBAC del
 * TicketService manual).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAutoService {

    private static final String SISTEMA_INVITADO_DOC = "SISTEMA_INVITADO";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_EN_CURSO = "EN_CURSO";

    private final VehiculoRepository vehiculoRepo;
    private final TicketRepository ticketRepo;
    private final PuntoParqueoRepository puntoParqueoRepo;
    private final ParqueaderoRepository parqueaderoRepo;
    private final TarifaRepository tarifaRepo;
    private final TipoVehiculoRepository tipoVehiculoRepo;
    private final PersonaRepository personaRepo;
    private final EstadoRepository estadoRepo;
    private final TarifaCalculatorService tarifaCalculator;
    private final ApplicationEventPublisher publisher;

    public enum Accion {
        ENTRADA_REGISTRADA,
        ENTRADA_DUPLICADA,
        SALIDA_REGISTRADA,
        SALIDA_SIN_TICKET,
        SOLO_DETECCION,        // tipo SEGURIDAD
        ERROR
    }

    public record AutoActionResult(
            Accion accion,
            Long ticketId,
            Long vehiculoId,
            Boolean vehiculoCreado,
            Long puntoParqueoId,
            Double montoCalculado,
            String mensaje
    ) {}

    /**
     * Procesa una placa detectada. Llamada desde OcrEventListener async.
     * NUNCA lanza excepcion: si falla, retorna AutoActionResult con accion=ERROR.
     */
    @Transactional
    public AutoActionResult procesarPlacaDetectada(
            Long camaraId, Long parqueaderoId, String tipoCamara, String placa) {

        try {
            if ("SEGURIDAD".equalsIgnoreCase(tipoCamara) || tipoCamara == null) {
                return new AutoActionResult(Accion.SOLO_DETECCION, null, null, null,
                        null, null, "Camara de seguridad — sin accion");
            }

            Parqueadero parqueadero = parqueaderoRepo.findById(parqueaderoId).orElse(null);
            if (parqueadero == null) {
                log.warn("Parqueadero {} no existe para placa {}", parqueaderoId, placa);
                return new AutoActionResult(Accion.ERROR, null, null, null, null, null,
                        "Parqueadero no encontrado");
            }

            if ("ENTRADA".equalsIgnoreCase(tipoCamara)) {
                return procesarEntrada(parqueadero, placa);
            }
            if ("SALIDA".equalsIgnoreCase(tipoCamara)) {
                return procesarSalida(parqueadero, placa);
            }

            return new AutoActionResult(Accion.SOLO_DETECCION, null, null, null, null, null,
                    "TipoCamara desconocido: " + tipoCamara);
        } catch (Exception e) {
            log.error("Error procesando placa {} en parq {}: {}", placa, parqueaderoId, e.getMessage(), e);
            return new AutoActionResult(Accion.ERROR, null, null, null, null, null, e.getMessage());
        }
    }

    // ── ENTRADA ────────────────────────────────────────────────────────────

    private AutoActionResult procesarEntrada(Parqueadero parqueadero, String placa) {
        boolean vehiculoCreado = false;
        Vehiculo vehiculo = vehiculoRepo.findByPlaca(placa).orElse(null);
        if (vehiculo == null) {
            vehiculo = crearVehiculoInvitado(placa);
            vehiculoCreado = true;
            log.info("Creado vehiculo invitado para placa {}", placa);
        }

        // ¿Ya tiene ticket EN_CURSO en este parqueadero?
        boolean yaAbierto = ticketRepo.findByVehiculoId(vehiculo.getId()).stream()
                .anyMatch(t -> ESTADO_EN_CURSO.equals(t.getEstado())
                        && t.getParqueadero() != null
                        && parqueadero.getId().equals(t.getParqueadero().getId()));
        if (yaAbierto) {
            return new AutoActionResult(Accion.ENTRADA_DUPLICADA, null, vehiculo.getId(),
                    vehiculoCreado, null, null,
                    "Vehiculo ya tiene ticket abierto en este parqueadero");
        }

        PuntoParqueo puntoLibre = buscarPuntoLibre(parqueadero.getId());
        if (puntoLibre == null) {
            return new AutoActionResult(Accion.ERROR, null, vehiculo.getId(), vehiculoCreado,
                    null, null, "No hay puntos de parqueo libres");
        }

        Tarifa tarifa = buscarTarifa(parqueadero.getId(), vehiculo.getTipoVehiculo());
        if (tarifa == null) {
            return new AutoActionResult(Accion.ERROR, null, vehiculo.getId(), vehiculoCreado,
                    puntoLibre.getId(), null,
                    "No hay tarifa configurada para este parqueadero/tipo de vehiculo");
        }

        Ticket t = new Ticket();
        t.setParqueadero(parqueadero);
        t.setPuntoParqueo(puntoLibre);
        t.setVehiculo(vehiculo);
        t.setTarifa(tarifa);
        t.setFechaHoraEntrada(LocalDateTime.now());
        t.setEstado(ESTADO_EN_CURSO);
        Ticket saved = ticketRepo.save(t);

        publisher.publishEvent(new TicketCreadoEvent(
                this, saved.getId(), parqueadero.getId(), puntoLibre.getId()));

        return new AutoActionResult(Accion.ENTRADA_REGISTRADA, saved.getId(),
                vehiculo.getId(), vehiculoCreado, puntoLibre.getId(), null,
                "Entrada registrada automaticamente");
    }

    // ── SALIDA ─────────────────────────────────────────────────────────────

    private AutoActionResult procesarSalida(Parqueadero parqueadero, String placa) {
        Vehiculo vehiculo = vehiculoRepo.findByPlaca(placa).orElse(null);
        if (vehiculo == null) {
            return new AutoActionResult(Accion.SALIDA_SIN_TICKET, null, null, false, null, null,
                    "Vehiculo no registrado — no hay ticket de entrada para esta placa");
        }

        Optional<Ticket> ticketAbierto = ticketRepo.findByVehiculoId(vehiculo.getId()).stream()
                .filter(t -> ESTADO_EN_CURSO.equals(t.getEstado())
                        && t.getParqueadero() != null
                        && parqueadero.getId().equals(t.getParqueadero().getId()))
                .findFirst();

        if (ticketAbierto.isEmpty()) {
            return new AutoActionResult(Accion.SALIDA_SIN_TICKET, null, vehiculo.getId(),
                    false, null, null,
                    "No hay ticket abierto del vehiculo en este parqueadero");
        }

        Ticket t = ticketAbierto.get();
        LocalDateTime salida = LocalDateTime.now();
        double monto = tarifaCalculator.calcular(t, salida);
        t.setFechaHoraSalida(salida);
        t.setMontoCalculado(monto);
        t.setEstado("CERRADO");
        Ticket saved = ticketRepo.save(t);

        Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
        publisher.publishEvent(new TicketCerradoEvent(
                this, saved.getId(), parqueadero.getId(), puntoId));

        return new AutoActionResult(Accion.SALIDA_REGISTRADA, saved.getId(),
                vehiculo.getId(), false, puntoId, monto, "Salida registrada automaticamente");
    }

    // ── HELPERS ────────────────────────────────────────────────────────────

    /** Busca el primer punto del parqueadero que no este archivado y sin ticket EN_CURSO. */
    private PuntoParqueo buscarPuntoLibre(Long parqueaderoId) {
        List<PuntoParqueo> puntos = puntoParqueoRepo.findBySubSeccionSeccionParqueaderoEmpresaId(
                parqueaderoRepo.findById(parqueaderoId)
                        .map(p -> p.getEmpresa() != null ? p.getEmpresa().getId() : null)
                        .orElse(-1L));
        for (PuntoParqueo pp : puntos) {
            // mismo parqueadero
            if (pp.getSubSeccion() == null
                    || pp.getSubSeccion().getSeccion() == null
                    || pp.getSubSeccion().getSeccion().getParqueadero() == null
                    || !parqueaderoId.equals(pp.getSubSeccion().getSeccion().getParqueadero().getId())) {
                continue;
            }
            // estado activo (no archivado)
            if (pp.getEstado() != null && "ARCHIVADO".equals(pp.getEstado().getNombre())) {
                continue;
            }
            // sin ticket EN_CURSO
            if (!ticketRepo.existsByPuntoParqueoIdAndEstado(pp.getId(), ESTADO_EN_CURSO)) {
                return pp;
            }
        }
        return null;
    }

    /** Busca primera tarifa del parqueadero, preferentemente del tipo de vehiculo del veh. */
    private Tarifa buscarTarifa(Long parqueaderoId, TipoVehiculo tipoVehiculo) {
        List<Tarifa> tarifas = tarifaRepo.findByParqueaderoId(parqueaderoId);
        if (tarifas.isEmpty()) return null;
        if (tipoVehiculo != null) {
            for (Tarifa t : tarifas) {
                if (t.getTipoVehiculo() != null
                        && tipoVehiculo.getId().equals(t.getTipoVehiculo().getId())) {
                    return t;
                }
            }
        }
        return tarifas.get(0);
    }

    /** Crea un vehiculo "invitado" con persona generica y tipo AUTO. */
    private Vehiculo crearVehiculoInvitado(String placa) {
        Persona persona = personaRepo.findAll().stream()
                .filter(p -> SISTEMA_INVITADO_DOC.equals(p.getNumeroDocumento()))
                .findFirst()
                .orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setNombre("Invitado");
                    nueva.setApellido("Sistema");
                    nueva.setTipoDocumento("SISTEMA");
                    nueva.setNumeroDocumento(SISTEMA_INVITADO_DOC);
                    return personaRepo.save(nueva);
                });

        TipoVehiculo tipo = tipoVehiculoRepo.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay tipos de vehiculo en BD"));

        Vehiculo v = new Vehiculo();
        v.setPersona(persona);
        v.setTipoVehiculo(tipo);
        v.setPlaca(placa);
        v.setColor("Desconocido");
        return vehiculoRepo.save(v);
    }
}
