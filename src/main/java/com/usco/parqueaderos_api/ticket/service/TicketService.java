package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.common.event.TicketPuntoCambiadoEvent;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
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
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUser;
    private final TarifaCalculatorService tarifaCalculator;

    @Transactional(readOnly = true)
    public List<TicketDTO> findAll() {
        List<Ticket> base;
        if (currentUser.isSuperAdmin()) {
            base = ticketRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = ticketRepository.findByParqueaderoEmpresaId(empresaId);
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
    public TicketDTO save(TicketDTO dto) {
        // 1) RBAC
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
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

        Ticket entity = new Ticket();
        entity.setParqueadero(parqueadero);
        entity.setPuntoParqueo(punto);
        entity.setVehiculo(vehiculo);
        entity.setTarifa(tarifa);
        entity.setFechaHoraEntrada(LocalDateTime.now()); // server time, ignorar dto
        entity.setFechaHoraSalida(null);
        entity.setEstado("EN_CURSO");
        entity.setMontoCalculado(null); // se calcula al cerrar

        Ticket saved = ticketRepository.save(entity);
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
    public TicketDTO update(Long id, TicketDTO dto) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo el operador puede modificar tickets");
        }

        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        // Multi-tenant
        if (existing.getParqueadero() != null && existing.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
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
            double monto = tarifaCalculator.calcular(existing, salida);
            existing.setFechaHoraSalida(salida);
            existing.setMontoCalculado(monto);
            existing.setEstado("CERRADO");
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
    public void delete(Long id) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede eliminar tickets");
        }
        ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        ticketRepository.deleteById(id);
    }

    /**
     * Registra la salida de un ticket EN_CURSO. Endpoint dedicado y limpio:
     * - Usa el reloj del servidor para fechaHoraSalida (anti-fraude)
     * - Calcula el monto via TarifaCalculatorService
     * - Solo permite cerrar tickets en estado EN_CURSO
     * - Publica TicketCerradoEvent para que el sistema actualice ocupacion
     */
    @Transactional
    public TicketDTO registrarSalida(Long id) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo el operador puede registrar salidas");
        }
        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        if (existing.getParqueadero() != null && existing.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
        }

        if (!"EN_CURSO".equals(existing.getEstado())) {
            throw new BusinessException(
                    "Solo se puede registrar salida de un ticket EN_CURSO. Estado actual: " + existing.getEstado(),
                    "ERR_INVALID_TRANSITION");
        }

        LocalDateTime salida = LocalDateTime.now();
        existing.setFechaHoraSalida(salida);
        existing.setMontoCalculado(tarifaCalculator.calcular(existing, salida));
        existing.setEstado("CERRADO");
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
    public TicketDTO cambiarPunto(Long ticketId, Long nuevoPuntoId) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
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

    private TicketDTO toDTO(Ticket e) {
        TicketDTO dto = new TicketDTO();
        dto.setId(e.getId());
        dto.setFechaHoraEntrada(e.getFechaHoraEntrada());
        dto.setFechaHoraSalida(e.getFechaHoraSalida());
        dto.setEstado(e.getEstado());
        dto.setMontoCalculado(e.getMontoCalculado());
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
