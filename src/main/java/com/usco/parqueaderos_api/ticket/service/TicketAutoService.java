package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.ticket.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.ticket.event.TicketCreadoEvent;
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
    private static final String ESTADO_EN_CURSO = "EN_CURSO";

    private final VehiculoRepository vehiculoRepo;
    private final TicketRepository ticketRepo;
    private final PuntoParqueoRepository puntoParqueoRepo;
    private final ParqueaderoRepository parqueaderoRepo;
    private final TarifaRepository tarifaRepo;
    private final TipoVehiculoRepository tipoVehiculoRepo;
    private final PersonaRepository personaRepo;
    private final TarifaCalculatorService tarifaCalculator;
    private final CobroOrchestrator cobroOrchestrator;
    private final ApplicationEventPublisher publisher;

    public enum Accion {
        ENTRADA_REGISTRADA,
        ENTRADA_DUPLICADA,
        SALIDA_REGISTRADA,
        SALIDA_CONFIRMADA_FISICA,  // ticket ya estaba cerrado por el admin; la camara confirma la salida fisica
        SALIDA_SIN_TICKET,
        SOLO_DETECCION,            // tipo SEGURIDAD
        ERROR
    }

    /** Ventana en segundos para considerar que la salida fisica corresponde a un cierre manual reciente. */
    private static final long SALIDA_FISICA_WINDOW_SEC = 5L * 60L;

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
            try {
                vehiculo = crearVehiculoInvitado(placa);
                vehiculoCreado = true;
                log.info("Creado vehiculo invitado para placa {}", placa);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Race condition: otro thread con misma placa nos gano. Reusar.
                vehiculo = vehiculoRepo.findByPlaca(placa).orElseThrow(() -> ex);
            }
        }

        // ¿Ya tiene ticket EN_CURSO en este parqueadero? (query exacta, no carga historial)
        if (ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(
                vehiculo.getId(), parqueadero.getId(), ESTADO_EN_CURSO)) {
            return new AutoActionResult(Accion.ENTRADA_DUPLICADA, null, vehiculo.getId(),
                    vehiculoCreado, null, null,
                    "Vehiculo ya tiene ticket abierto en este parqueadero");
        }

        // Buscar punto libre + lock pesimista para serializar entradas concurrentes
        PuntoParqueo puntoLibre = buscarYLockearPuntoLibre(parqueadero.getId());
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

        Ticket saved;
        try {
            saved = ticketRepo.save(t);
            // Force flush para que la violacion del indice unico se dispare AQUI y no en commit
            ticketRepo.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Race condition: otro thread creo el ticket en paralelo. El indice
            // uniq_ticket_vehiculo_parqueadero_en_curso o uniq_ticket_punto_en_curso
            // bloqueo la insercion. Retornamos ENTRADA_DUPLICADA en lugar de propagar.
            log.warn("Race condition al crear ticket (placa={}, parq={}): {}",
                    placa, parqueadero.getId(), ex.getMostSpecificCause().getMessage());
            return new AutoActionResult(Accion.ENTRADA_DUPLICADA, null, vehiculo.getId(),
                    vehiculoCreado, null, null,
                    "Vehiculo ya tiene ticket abierto (detectado via constraint)");
        }

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

        // Caso 1: ticket EN_CURSO -> ciclo normal, cierra y cobra
        Optional<Ticket> ticketAbierto = ticketRepo
                .findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
                        vehiculo.getId(), parqueadero.getId(), ESTADO_EN_CURSO);

        if (ticketAbierto.isPresent()) {
            Ticket t = ticketAbierto.get();
            LocalDateTime salida = LocalDateTime.now();
            com.usco.parqueaderos_api.ticket.service.strategy.CobroResult cobro =
                    cobroOrchestrator.cobrar(t, salida);
            t.setFechaHoraSalida(salida);
            t.setFechaHoraSalidaFisica(salida);
            t.setMontoCalculado(cobro.montoCobrado());
            t.setSuscripcionId(cobro.suscripcionId());
            t.setEstado("CERRADO");
            Ticket saved = ticketRepo.save(t);

            Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
            publisher.publishEvent(new TicketCerradoEvent(
                    this, saved.getId(), parqueadero.getId(), puntoId));

            return new AutoActionResult(Accion.SALIDA_REGISTRADA, saved.getId(),
                    vehiculo.getId(), false, puntoId, cobro.montoCobrado(),
                    cobro.mensaje() != null ? cobro.mensaje() : "Salida registrada automaticamente");
        }

        // Caso 2: ticket CERRADO recientemente (cierre manual del admin) -> salida fisica esperada
        Optional<Ticket> ultimo = ticketRepo
                .findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
                        vehiculo.getId(), parqueadero.getId());
        if (ultimo.isPresent() && "CERRADO".equals(ultimo.get().getEstado())
                && ultimo.get().getFechaHoraSalida() != null) {
            Ticket t = ultimo.get();
            LocalDateTime ahora = LocalDateTime.now();
            long segundosDesdeCierre = java.time.Duration.between(
                    t.getFechaHoraSalida(), ahora).getSeconds();

            if (segundosDesdeCierre <= SALIDA_FISICA_WINDOW_SEC) {
                // Si no se habia registrado salida fisica antes, la registramos
                if (t.getFechaHoraSalidaFisica() == null) {
                    t.setFechaHoraSalidaFisica(ahora);
                    ticketRepo.save(t);
                }
                Long puntoId = t.getPuntoParqueo() != null ? t.getPuntoParqueo().getId() : null;
                return new AutoActionResult(Accion.SALIDA_CONFIRMADA_FISICA, t.getId(),
                        vehiculo.getId(), false, puntoId, t.getMontoCalculado(),
                        "Salida fisica confirmada — ticket ya estaba cerrado");
            }
        }

        // Caso 3: sin ticket reciente -> alerta de salida fantasma
        return new AutoActionResult(Accion.SALIDA_SIN_TICKET, null, vehiculo.getId(),
                false, null, null,
                "No hay ticket reciente del vehiculo en este parqueadero");
    }

    // ── HELPERS ────────────────────────────────────────────────────────────

    /**
     * Busca el primer punto del parqueadero tipo "publico" (no administrativo,
     * no discapacitado), no archivado y sin ticket EN_CURSO, y le aplica
     * lock pesimista (FOR UPDATE) para serializar entradas concurrentes.
     *
     * Tipos auto-asignables: nombre del TipoPuntoParqueo contiene "plac",
     * "publi", "general" o "normal" (lower-case). Puntos sin tipo NO se
     * autoasignan (conservador para no meter invitados en puntos especiales).
     *
     * Si dos cámaras ENTRADA disparan en paralelo: la primera obtiene el lock
     * sobre el punto, hace existsBy=false, crea el ticket. La segunda espera
     * el lock; cuando lo recibe, existsBy=true y pasa al siguiente punto.
     */
    private PuntoParqueo buscarYLockearPuntoLibre(Long parqueaderoId) {
        List<PuntoParqueo> candidatos = puntoParqueoRepo.findActiveByParqueaderoId(parqueaderoId);
        for (PuntoParqueo pp : candidatos) {
            if (!esTipoPublico(pp)) continue;
            // Lock pesimista + re-verificacion bajo el lock
            Optional<PuntoParqueo> locked = puntoParqueoRepo.findByIdForUpdate(pp.getId());
            if (locked.isEmpty()) continue;
            if (!ticketRepo.existsByPuntoParqueoIdAndEstado(locked.get().getId(), ESTADO_EN_CURSO)) {
                return locked.get();
            }
        }
        return null;
    }

    private boolean esTipoPublico(PuntoParqueo pp) {
        // Conservador: punto sin tipo NO se autoasigna (puede ser reservado/admin sin etiquetar).
        if (pp.getTipoPuntoParqueo() == null) return false;
        String n = pp.getTipoPuntoParqueo().getNombre();
        if (n == null) return false;
        n = n.toLowerCase();
        return n.contains("plac") || n.contains("publi") || n.contains("general") || n.contains("normal");
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

    /** Crea un vehiculo "invitado" con persona generica y tipo default. */
    private Vehiculo crearVehiculoInvitado(String placa) {
        Persona persona = personaRepo.findByNumeroDocumento(SISTEMA_INVITADO_DOC)
                .orElseGet(() -> {
                    Persona nueva = new Persona();
                    nueva.setNombre("Invitado");
                    nueva.setApellido("Sistema");
                    nueva.setTipoDocumento("SISTEMA");
                    nueva.setNumeroDocumento(SISTEMA_INVITADO_DOC);
                    return personaRepo.save(nueva);
                });

        TipoVehiculo tipo = tipoVehiculoRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No hay tipos de vehiculo en BD"));

        Vehiculo v = new Vehiculo();
        v.setPersona(persona);
        v.setTipoVehiculo(tipo);
        v.setPlaca(placa);
        v.setColor("Desconocido");
        return vehiculoRepo.save(v);
    }
}
