package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.audit.aspect.Auditable;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.ticket.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.ticket.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.ticket.event.TicketPuntoCambiadoEvent;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.ticket.dto.TicketDTO;
import com.usco.parqueaderos_api.ticket.dto.TicketManualDTO;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final PuntoParqueoRepository puntoParqueoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TarifaRepository tarifaRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUser;
    private final TarifaCalculatorService tarifaCalculator;
    private final CobroOrchestrator cobroOrchestrator;
    private final com.usco.parqueaderos_api.billing.repository.FacturaRepository facturaRepository;
    private final com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository suscripcionRepository;
    private final com.usco.parqueaderos_api.user.repository.PersonaRepository personaRepository;

    /**
     * Roles con privilegio operativo sobre tickets:
     * OPERARIO_CAJA, ADMIN_PARQUEADERO, ADMIN, SUPER_ADMIN.
     * USER queda fuera (solo puede ver sus propios tickets via findById).
     */
    private boolean puedeOperarTickets() {
        return currentUser.isSuperAdmin()
                || currentUser.isAdmin()
                || currentUser.isAdminParqueadero()
                || currentUser.isOperarioCaja();
    }

    /**
     * Roles con privilegio de anular tickets. Incluye OPERARIO_CAJA: la auditoria
     * universal (audit_log + motivo obligatorio min 10 chars) deja trazabilidad
     * inmutable de cada anulacion, asi que es seguro permitirles a los operarios
     * el caso comun "cliente perdio ticket / doble entrada por OCR / error de
     * digitacion". Si despues se detecta abuso, el log identifica al responsable.
     */
    private boolean puedeAnularTickets() {
        return puedeOperarTickets();
    }

    @Transactional(readOnly = true)
    public List<TicketDTO> findAll() {
        List<Ticket> base;
        if (currentUser.isSuperAdmin()) {
            base = ticketRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = ticketRepository.findByParqueaderoEmpresaId(empresaId);
        } else if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            List<Long> parqIds = currentUser.getParqueaderoIds();
            if (parqIds.isEmpty()) return Collections.emptyList();
            base = ticketRepository.findByParqueaderoIdIn(parqIds);
        } else {
            base = ticketRepository.findByVehiculoPersonaId(currentUser.getCurrentPersonaId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketDTO findById(Long id) {
        Ticket t = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        if (currentUser.isSuperAdmin()) return toDTO(t);
        if (currentUser.isAdmin()) {
            if (t.getParqueadero() != null && t.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(t.getParqueadero().getEmpresa().getId());
            }
        } else if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            Long parqId = t.getParqueadero() != null ? t.getParqueadero().getId() : null;
            if (parqId == null) throw new AccessDeniedException("Ticket sin parqueadero asociado");
            currentUser.requireParqueadero(parqId);
        } else {
            Long personaId = t.getVehiculo() != null && t.getVehiculo().getPersona() != null
                    ? t.getVehiculo().getPersona().getId() : null;
            currentUser.requireOwnerOrAnyAdmin(personaId);
        }
        return toDTO(t);
    }

    /**
     * Crea un ticket de entrada. Blindado contra:
     * - RBAC: solo ADMIN/SUPER_ADMIN registran entradas
     * - Multi-tenant: el parqueadero debe ser de la empresa del operador
     * - Race condition: lock pesimista sobre el punto + verificacion de
     *   estado + indice unico parcial en BD
     * - Inyeccion: el cliente NO puede setear estado, fecha, monto
     */
    @Transactional
    @Auditable(tabla = "ticket", accion = "CREATE")
    public TicketDTO save(TicketDTO dto) {
        // 1) RBAC: ADMIN, SUPER_ADMIN, ADMIN_PARQUEADERO, OPERARIO_CAJA
        if (!puedeOperarTickets()) {
            throw new AccessDeniedException("Solo el operador puede registrar entradas");
        }

        // 2) Validar parqueadero y multi-tenant
        if (dto.getParqueaderoId() == null || dto.getPuntoParqueoId() == null
                || dto.getVehiculoId() == null || dto.getTarifaId() == null) {
            throw new BusinessException(
                    "parqueaderoId, puntoParqueoId, vehiculoId y tarifaId son obligatorios",
                    "ERR_MISSING_FIELDS");
        }
        Parqueadero parqueadero = findParqueadero(dto.getParqueaderoId());
        if (parqueadero.getEmpresa() != null) {
            currentUser.requireEmpresa(parqueadero.getEmpresa().getId());
        }
        // ADMIN_PARQUEADERO y OPERARIO_CAJA: ademas validar que el parqueadero
        // este en su lista de asignados.
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            currentUser.requireParqueadero(parqueadero.getId());
        }

        // 3) Lock pesimista del punto (serializa intentos concurrentes)
        PuntoParqueo punto = puntoParqueoRepository.findByIdForUpdate(dto.getPuntoParqueoId())
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", dto.getPuntoParqueoId()));

        // 4) Validar que el punto NO tenga ticket EN_CURSO
        if (ticketRepository.existsByPuntoParqueoIdAndEstado(punto.getId(), "EN_CURSO")) {
            throw new BusinessException(
                    "El punto de parqueo ya esta ocupado por un vehiculo en curso",
                    "ERR_POINT_OCCUPIED");
        }

        // 5) Sanitizacion: forzar valores confiables, IGNORAR los del DTO sensibles
        Vehiculo vehiculo = findVehiculo(dto.getVehiculoId());
        Tarifa tarifa = findTarifa(dto.getTarifaId());

        // 4.b) Si el punto esta reservado por una suscripcion ACTIVA, solo el
        //      vehiculo de esa suscripcion puede usarlo.
        com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository suscRepo =
                this.suscripcionRepository;
        if (suscRepo != null) {
            suscRepo.findActivaByPuntoReservado(punto.getId()).ifPresent(s -> {
                if (s.getVehiculo() == null
                        || !s.getVehiculo().getId().equals(vehiculo.getId())) {
                    throw new BusinessException(
                            "El punto esta reservado por la suscripcion #" + s.getId()
                                    + " (placa " + (s.getVehiculo() != null ? s.getVehiculo().getPlaca() : "?") + ")",
                            "ERR_PUNTO_RESERVADO_SUSCRIPCION");
                }
            });
        }

        // 5.b) Validar que el vehiculo no tenga ya un ticket EN_CURSO en este parqueadero.
        //      Defensa programatica + indice unico parcial en BD como red de seguridad.
        if (ticketRepository.existsByVehiculoIdAndParqueaderoIdAndEstado(
                vehiculo.getId(), parqueadero.getId(), "EN_CURSO")) {
            throw new BusinessException(
                    "El vehiculo ya tiene un ticket EN_CURSO en este parqueadero",
                    "ERR_TICKET_DUPLICADO");
        }

        Ticket entity = new Ticket();
        entity.setParqueadero(parqueadero);
        entity.setPuntoParqueo(punto);
        entity.setVehiculo(vehiculo);
        entity.setTarifa(tarifa);
        entity.setFechaHoraEntrada(LocalDateTime.now()); // server time, ignorar dto
        entity.setFechaHoraSalida(null);
        entity.setEstado("EN_CURSO");
        entity.setMontoCalculado(null); // se calcula al cerrar
        // Snapshot economico de la tarifa al momento de la entrada (anti-fraude)
        entity.setTarifaValorSnapshot(tarifa.getValor());
        entity.setTarifaUnidadSnapshot(tarifa.getUnidad());
        entity.setTarifaMinimoSnapshot(tarifa.getValorMinimo());
        entity.setTarifaGraciaSnapshot(tarifa.getMinutosGracia());
        entity.setTarifaCubreSnapshot(tarifa.getMinutosCubiertosPorMinimo());
        entity.setCreadoPorUsuarioId(currentUser.getCurrentUserId());

        Ticket saved;
        try {
            saved = ticketRepository.save(entity);
            ticketRepository.flush(); // forzar la insercion AHORA para atrapar la violacion del indice
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Race condition: otro thread inserto en paralelo. El indice unico parcial bloqueo.
            throw new BusinessException(
                    "El vehiculo ya tiene un ticket EN_CURSO en este parqueadero "
                    + "(detectado por constraint)",
                    "ERR_TICKET_DUPLICADO");
        }

        eventPublisher.publishEvent(
                new TicketCreadoEvent(this, saved.getId(), parqueadero.getId(), punto.getId()));
        return toDTO(saved);
    }

    /**
     * Actualiza un ticket. Solo permite:
     * - Cerrar el ticket (transicion EN_CURSO -> CERRADO): el backend setea
     *   fecha de salida y monto calculado de forma autoritativa.
     * - Anular un ticket (estado ANULADO).
     *
     * NUNCA acepta montoCalculado desde el cliente.
     */
    @Transactional
    @Auditable(tabla = "ticket", accion = "UPDATE")
    public TicketDTO update(Long id, TicketDTO dto) {
        if (!puedeOperarTickets()) {
            throw new AccessDeniedException("Solo el operador puede modificar tickets");
        }

        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        // Multi-tenant
        if (existing.getParqueadero() != null && existing.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            Long parqId = existing.getParqueadero() != null ? existing.getParqueadero().getId() : null;
            if (parqId == null) throw new AccessDeniedException("Ticket sin parqueadero asociado");
            currentUser.requireParqueadero(parqId);
        }

        String estadoAnterior = existing.getEstado();
        String estadoNuevo = dto.getEstado();

        if (estadoNuevo == null || estadoNuevo.equals(estadoAnterior)) {
            // No-op
            return toDTO(existing);
        }

        // Solo permitimos transiciones validas: EN_CURSO -> CERRADO o EN_CURSO -> ANULADO
        if (!"EN_CURSO".equals(estadoAnterior)) {
            throw new BusinessException(
                    "Solo se puede modificar un ticket EN_CURSO. Estado actual: " + estadoAnterior,
                    "ERR_INVALID_TRANSITION");
        }

        if ("CERRADO".equals(estadoNuevo)) {
            LocalDateTime salida = LocalDateTime.now();
            com.usco.parqueaderos_api.ticket.service.strategy.CobroResult cobro =
                    cobroOrchestrator.cobrar(existing, salida);
            existing.setFechaHoraSalida(salida);
            existing.setMontoCalculado(cobro.montoCobrado());
            existing.setSuscripcionId(cobro.suscripcionId());
            existing.setEstado("CERRADO");
            existing.setCerradoPorUsuarioId(currentUser.getCurrentUserId());
        } else if ("ANULADO".equals(estadoNuevo)) {
            existing.setEstado("ANULADO");
            // No se calcula monto, no hay cobro
        } else {
            throw new BusinessException(
                    "Estado destino invalido: " + estadoNuevo,
                    "ERR_INVALID_STATE");
        }

        Ticket saved = ticketRepository.save(existing);
        if ("CERRADO".equals(saved.getEstado())) {
            Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
            Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
            eventPublisher.publishEvent(new TicketCerradoEvent(this, saved.getId(), parqueaderoId, puntoId));
        }
        return toDTO(saved);
    }

    @Transactional
    @Auditable(tabla = "ticket", accion = "DELETE_FISICO", requiereMotivo = true)
    public void delete(Long id) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede eliminar tickets");
        }
        ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        ticketRepository.deleteById(id);
    }

    /**
     * Anula un ticket EN_CURSO con motivo obligatorio. Si tiene factura
     * asociada PENDIENTE, la anula tambien. Si el ticket fue cobrado contra
     * un saldo ABONO_PREPAGO, ese descuento se reversa.
     *
     * Caso de uso: cliente perdio el ticket fisico, doble entrada por OCR, etc.
     */
    @Transactional
    @Auditable(tabla = "ticket", accion = "ANULAR", requiereMotivo = true)
    public TicketDTO anular(Long id, String motivo) {
        // RBAC: los 4 roles operativos pueden anular. La auditoria + motivo
        // obligatorio garantizan trazabilidad inmutable de quien anulo y por que.
        if (!puedeAnularTickets()) {
            throw new AccessDeniedException("Solo el operador puede anular tickets");
        }
        if (motivo == null || motivo.trim().length() < 10) {
            throw new BusinessException(
                    "Motivo obligatorio (min 10 caracteres)",
                    "ERR_MISSING_FIELDS");
        }
        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        if (existing.getParqueadero() != null && existing.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            Long parqId = existing.getParqueadero() != null ? existing.getParqueadero().getId() : null;
            if (parqId == null) throw new AccessDeniedException("Ticket sin parqueadero asociado");
            currentUser.requireParqueadero(parqId);
        }
        if (!"EN_CURSO".equals(existing.getEstado()) && !"CERRADO".equals(existing.getEstado())) {
            throw new BusinessException(
                    "Solo se puede anular un ticket EN_CURSO o CERRADO. Estado: " + existing.getEstado(),
                    "ERR_INVALID_TRANSITION");
        }
        existing.setEstado("ANULADO");
        existing.setMotivoAnulacion(motivo);
        existing.setAnuladoEn(LocalDateTime.now());
        existing.setAnuladoPorUsuarioId(currentUser.getCurrentUserId());
        Ticket saved = ticketRepository.save(existing);

        // Anular factura asociada PENDIENTE (no toca PAGADAs - esas requieren reverso de pago)
        com.usco.parqueaderos_api.billing.repository.FacturaRepository facturaRepo =
                facturaRepository;
        if (facturaRepo != null) {
            facturaRepo.findByTicketId(id).forEach(f -> {
                if ("PENDIENTE".equals(f.getEstado())) {
                    f.setEstado("ANULADA");
                    facturaRepo.save(f);
                }
            });
        }

        // Publicar TicketCerradoEvent para liberar el punto (mismo efecto)
        Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
        Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
        eventPublisher.publishEvent(new TicketCerradoEvent(this, saved.getId(), parqueaderoId, puntoId));
        return toDTO(saved);
    }

    /**
     * Registra la salida de un ticket EN_CURSO. Endpoint dedicado y limpio:
     * - Usa el reloj del servidor para fechaHoraSalida (anti-fraude)
     * - Calcula el monto via TarifaCalculatorService
     * - Solo permite cerrar tickets en estado EN_CURSO
     * - Publica TicketCerradoEvent para que el sistema actualice ocupacion
     */
    @Transactional
    @Auditable(tabla = "ticket", accion = "CERRAR")
    public TicketDTO registrarSalida(Long id) {
        if (!puedeOperarTickets()) {
            throw new AccessDeniedException("Solo el operador puede registrar salidas");
        }
        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        if (existing.getParqueadero() != null && existing.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            Long parqId = existing.getParqueadero() != null ? existing.getParqueadero().getId() : null;
            if (parqId == null) throw new AccessDeniedException("Ticket sin parqueadero asociado");
            currentUser.requireParqueadero(parqId);
        }

        if (!"EN_CURSO".equals(existing.getEstado())) {
            throw new BusinessException(
                    "Solo se puede registrar salida de un ticket EN_CURSO. Estado actual: " + existing.getEstado(),
                    "ERR_INVALID_TRANSITION");
        }

        LocalDateTime salida = LocalDateTime.now();

        // Aplicar estrategia de cobro: suscripciones primero, fallback tarifa normal
        com.usco.parqueaderos_api.ticket.service.strategy.CobroResult cobro =
                cobroOrchestrator.cobrar(existing, salida);

        existing.setFechaHoraSalida(salida);
        existing.setMontoCalculado(cobro.montoCobrado());
        existing.setSuscripcionId(cobro.suscripcionId());
        existing.setEstado("CERRADO");
        existing.setCerradoPorUsuarioId(currentUser.getCurrentUserId());
        Ticket saved = ticketRepository.save(existing);

        Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
        Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
        eventPublisher.publishEvent(new TicketCerradoEvent(this, saved.getId(), parqueaderoId, puntoId));

        return toDTO(saved);
    }

    /**
     * Mueve un ticket EN_CURSO a otro punto de parqueo del mismo parqueadero.
     * Util cuando el OCR asigno automaticamente un punto y el operador necesita
     * indicar donde se estaciono realmente el vehiculo (sin sensores).
     *
     * - Solo ADMIN/SUPER_ADMIN
     * - El ticket debe estar EN_CURSO
     * - El nuevo punto debe estar en el mismo parqueadero, no archivado, sin otro ticket EN_CURSO
     * - Lock pesimista sobre el nuevo punto para evitar race conditions
     */
    @Transactional
    @Auditable(tabla = "ticket", accion = "CAMBIAR_PUNTO")
    public TicketDTO cambiarPunto(Long ticketId, Long nuevoPuntoId) {
        if (!puedeOperarTickets()) {
            throw new AccessDeniedException("Solo el operador puede mover tickets");
        }
        if (nuevoPuntoId == null) {
            throw new BusinessException("puntoParqueoId es obligatorio", "ERR_MISSING_FIELDS");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (ticket.getParqueadero() != null && ticket.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(ticket.getParqueadero().getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            Long parqId = ticket.getParqueadero() != null ? ticket.getParqueadero().getId() : null;
            if (parqId == null) throw new AccessDeniedException("Ticket sin parqueadero asociado");
            currentUser.requireParqueadero(parqId);
        }
        if (!"EN_CURSO".equals(ticket.getEstado())) {
            throw new BusinessException(
                    "Solo se puede mover un ticket EN_CURSO. Estado actual: " + ticket.getEstado(),
                    "ERR_INVALID_TRANSITION");
        }

        Long puntoAnteriorId = ticket.getPuntoParqueo() != null ? ticket.getPuntoParqueo().getId() : null;
        if (nuevoPuntoId.equals(puntoAnteriorId)) {
            return toDTO(ticket); // no-op
        }

        // Lock pesimista sobre el nuevo punto
        PuntoParqueo nuevoPunto = puntoParqueoRepository.findByIdForUpdate(nuevoPuntoId)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", nuevoPuntoId));

        // Mismo parqueadero
        Long parqueaderoTicket = ticket.getParqueadero() != null ? ticket.getParqueadero().getId() : null;
        Long parqueaderoPunto = nuevoPunto.getSubSeccion() != null
                && nuevoPunto.getSubSeccion().getSeccion() != null
                && nuevoPunto.getSubSeccion().getSeccion().getParqueadero() != null
                ? nuevoPunto.getSubSeccion().getSeccion().getParqueadero().getId()
                : null;
        if (parqueaderoTicket == null || !parqueaderoTicket.equals(parqueaderoPunto)) {
            throw new BusinessException(
                    "El punto destino debe pertenecer al mismo parqueadero del ticket",
                    "ERR_INVALID_STATE");
        }

        // Punto no archivado
        if (nuevoPunto.getEstado() != null && "ARCHIVADO".equals(nuevoPunto.getEstado().getNombre())) {
            throw new BusinessException("El punto destino esta archivado", "ERR_INVALID_STATE");
        }

        // Punto destino libre
        if (ticketRepository.existsByPuntoParqueoIdAndEstado(nuevoPuntoId, "EN_CURSO")) {
            throw new BusinessException(
                    "El punto destino ya esta ocupado por otro vehiculo",
                    "ERR_POINT_OCCUPIED");
        }

        ticket.setPuntoParqueo(nuevoPunto);
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publishEvent(new TicketPuntoCambiadoEvent(
                this, saved.getId(), parqueaderoTicket, puntoAnteriorId, nuevoPuntoId));

        return toDTO(saved);
    }

    /**
     * Crea un ticket de entrada SIN OCR. Caso de uso: el operario digita la placa
     * porque la camara no la pudo leer (vidrio sucio, mal iluminado, sin luz)
     * o el vehiculo no tiene placa visible.
     *
     * Logica:
     * - placa null/vacia: se genera "VIS-YYYYMMDD-HHMMSS-XXXX" y se crea Vehiculo
     *   esVisitante=true sin persona asociada.
     * - placa existe en BD: reusa ese Vehiculo.
     * - placa no existe: crea Vehiculo nuevo con esVisitante=true.
     *
     * Luego delega a save() para reutilizar TODA la validacion (RBAC, multi-tenant,
     * lock pesimista, suscripciones, anti-duplicado).
     */
    @Transactional
    public TicketDTO createManual(TicketManualDTO dto) {
        if (!puedeOperarTickets()) {
            throw new AccessDeniedException("Solo el operador puede registrar entradas");
        }
        if (dto.getParqueaderoId() == null || dto.getPuntoParqueoId() == null
                || dto.getTarifaId() == null || dto.getTipoVehiculoId() == null) {
            throw new BusinessException(
                    "parqueaderoId, puntoParqueoId, tarifaId y tipoVehiculoId son obligatorios",
                    "ERR_MISSING_FIELDS");
        }

        // Resolver vehiculo en 3 escenarios:
        //  a) Placa null/vacia -> autogenera "VIS-..." + esVisitante=true
        //  b) Placa existe en BD -> reusa (puede ser empresa o visitante previo)
        //  c) Placa nueva -> crea Vehiculo con personaId si vino, esVisitante=(personaId==null)
        String placaInput = dto.getPlaca() == null ? null : dto.getPlaca().trim().toUpperCase();
        final boolean sinPlaca = (placaInput == null || placaInput.isEmpty());
        final String placaFinal = sinPlaca ? generarPlacaVisitante() : placaInput;
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placaFinal).orElseGet(() ->
                crearVehiculoNuevo(placaFinal, dto.getTipoVehiculoId(), dto.getColor(),
                        sinPlaca ? null : dto.getPersonaId()));

        // Construir TicketDTO normal y delegar a save() (reusa toda la validacion)
        TicketDTO tdto = new TicketDTO();
        tdto.setParqueaderoId(dto.getParqueaderoId());
        tdto.setPuntoParqueoId(dto.getPuntoParqueoId());
        tdto.setTarifaId(dto.getTarifaId());
        tdto.setVehiculoId(vehiculo.getId());
        return save(tdto);
    }

    private String generarPlacaVisitante() {
        String stamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rand = (int) (Math.random() * 9000) + 1000;
        return "VIS-" + stamp + "-" + rand;
    }

    /**
     * Crea un Vehiculo nuevo durante registro manual.
     * - Si personaId != null: vehiculo asociado a persona, esVisitante=false (carro empresa).
     * - Si personaId == null: vehiculo visitante (esVisitante=true).
     */
    private Vehiculo crearVehiculoNuevo(String placa, Long tipoVehiculoId, String color,
                                         Long personaId) {
        TipoVehiculo tipo = tipoVehiculoRepository.findById(tipoVehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", tipoVehiculoId));
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setTipoVehiculo(tipo);
        v.setColor(color);
        v.setActivo(true);
        v.setUltimaActividad(LocalDateTime.now());
        if (personaId != null) {
            com.usco.parqueaderos_api.user.entity.Persona persona =
                    personaRepository.findById(personaId)
                            .orElseThrow(() -> new ResourceNotFoundException("Persona", personaId));
            v.setPersona(persona);
            v.setEsVisitante(false);
        } else {
            v.setEsVisitante(true);
        }
        return vehiculoRepository.save(v);
    }

    private TicketDTO toDTO(Ticket e) {
        TicketDTO dto = new TicketDTO();
        dto.setId(e.getId());
        dto.setFechaHoraEntrada(e.getFechaHoraEntrada());
        dto.setFechaHoraSalida(e.getFechaHoraSalida());
        dto.setFechaHoraSalidaFisica(e.getFechaHoraSalidaFisica());
        dto.setEstado(e.getEstado());
        dto.setMontoCalculado(e.getMontoCalculado());
        dto.setSuscripcionId(e.getSuscripcionId());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getVehiculo() != null) { dto.setVehiculoId(e.getVehiculo().getId()); dto.setVehiculoPlaca(e.getVehiculo().getPlaca()); }
        if (e.getPuntoParqueo() != null) { dto.setPuntoParqueoId(e.getPuntoParqueo().getId()); dto.setPuntoParqueoNombre(e.getPuntoParqueo().getNombre()); }
        if (e.getTarifa() != null) { dto.setTarifaId(e.getTarifa().getId()); dto.setTarifaNombre(e.getTarifa().getNombre()); }
        return dto;
    }

    private Parqueadero findParqueadero(Long id) {
        return parqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
    }
    private Vehiculo findVehiculo(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
    }
    private Tarifa findTarifa(Long id) {
        return tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", id));
    }
}
