package com.usco.parqueaderos_api.reservation.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.event.ReservaCanceladaEvent;
import com.usco.parqueaderos_api.common.event.ReservaCreadaEvent;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.reservation.dto.ReservaDTO;
import com.usco.parqueaderos_api.reservation.entity.Reserva;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final PuntoParqueoRepository puntoParqueoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<ReservaDTO> findAll() {
        List<Reserva> base;
        if (currentUser.isSuperAdmin()) {
            base = reservaRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = reservaRepository.findByParqueaderoEmpresaId(empresaId);
        } else {
            // USER: solo sus propias reservas
            base = reservaRepository.findByUsuarioId(currentUser.getCurrentUserId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservaDTO findById(Long id) {
        Reserva r = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
        Long ownerUserId = r.getUsuario() != null ? r.getUsuario().getId() : null;
        Long empresaId = r.getParqueadero() != null && r.getParqueadero().getEmpresa() != null
                ? r.getParqueadero().getEmpresa().getId() : null;
        currentUser.requireOwnerOrAdminEmpresa(ownerUserId, empresaId);
        return toDTO(r);
    }

    @Transactional
    public ReservaDTO save(ReservaDTO dto) {
        Reserva entity = toEntity(dto);
        if (entity.getEstado() == null) entity.setEstado("PENDIENTE");
        Reserva saved = reservaRepository.save(entity);
        Long usuarioId = saved.getUsuario() != null ? saved.getUsuario().getId() : null;
        Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
        eventPublisher.publishEvent(new ReservaCreadaEvent(this, saved.getId(), usuarioId, parqueaderoId));
        return toDTO(saved);
    }

    @Transactional
    public ReservaDTO update(Long id, ReservaDTO dto) {
        Reserva existing = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
        String estadoAnterior = existing.getEstado();
        existing.setFechaHoraInicio(dto.getFechaHoraInicio());
        existing.setFechaHoraFin(dto.getFechaHoraFin());
        existing.setEstado(dto.getEstado());
        if (dto.getUsuarioId() != null) existing.setUsuario(findUsuario(dto.getUsuarioId()));
        if (dto.getVehiculoId() != null) existing.setVehiculo(findVehiculo(dto.getVehiculoId()));
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getPuntoParqueoId() != null) existing.setPuntoParqueo(findPuntoParqueo(dto.getPuntoParqueoId()));
        Reserva saved = reservaRepository.save(existing);

        // Si paso a un estado "no activo", publicar evento para liberar el punto
        boolean eraActiva = "PENDIENTE".equals(estadoAnterior) || "CONFIRMADA".equals(estadoAnterior);
        boolean ahoraInactiva = "CANCELADA".equals(saved.getEstado()) || "EXPIRADA".equals(saved.getEstado());
        if (eraActiva && ahoraInactiva) {
            publicarCancelada(saved);
        }
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Reserva existing = reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
        // Capturar refs antes de borrar para el evento
        Long reservaId = existing.getId();
        Long usuarioId = existing.getUsuario() != null ? existing.getUsuario().getId() : null;
        Long parqueaderoId = existing.getParqueadero() != null ? existing.getParqueadero().getId() : null;
        Long puntoParqueoId = existing.getPuntoParqueo() != null ? existing.getPuntoParqueo().getId() : null;

        reservaRepository.deleteById(id);
        eventPublisher.publishEvent(
                new ReservaCanceladaEvent(this, reservaId, usuarioId, parqueaderoId, puntoParqueoId));
    }

    private void publicarCancelada(Reserva r) {
        Long usuarioId = r.getUsuario() != null ? r.getUsuario().getId() : null;
        Long parqueaderoId = r.getParqueadero() != null ? r.getParqueadero().getId() : null;
        Long puntoParqueoId = r.getPuntoParqueo() != null ? r.getPuntoParqueo().getId() : null;
        eventPublisher.publishEvent(
                new ReservaCanceladaEvent(this, r.getId(), usuarioId, parqueaderoId, puntoParqueoId));
    }

    private ReservaDTO toDTO(Reserva e) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(e.getId());
        dto.setFechaHoraInicio(e.getFechaHoraInicio());
        dto.setFechaHoraFin(e.getFechaHoraFin());
        dto.setEstado(e.getEstado());
        if (e.getUsuario() != null) { dto.setUsuarioId(e.getUsuario().getId()); dto.setUsuarioNombre(e.getUsuario().getCorreo()); }
        if (e.getVehiculo() != null) { dto.setVehiculoId(e.getVehiculo().getId()); dto.setVehiculoPlaca(e.getVehiculo().getPlaca()); }
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getPuntoParqueo() != null) { dto.setPuntoParqueoId(e.getPuntoParqueo().getId()); dto.setPuntoParqueoNombre(e.getPuntoParqueo().getNombre()); }
        return dto;
    }

    private Reserva toEntity(ReservaDTO dto) {
        Reserva e = new Reserva();
        e.setFechaHoraInicio(dto.getFechaHoraInicio());
        e.setFechaHoraFin(dto.getFechaHoraFin());
        e.setEstado(dto.getEstado());
        if (dto.getUsuarioId() != null) e.setUsuario(findUsuario(dto.getUsuarioId()));
        if (dto.getVehiculoId() != null) e.setVehiculo(findVehiculo(dto.getVehiculoId()));
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getPuntoParqueoId() != null) e.setPuntoParqueo(findPuntoParqueo(dto.getPuntoParqueoId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private PuntoParqueo findPuntoParqueo(Long id) { return puntoParqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id)); }
    private Usuario findUsuario(Long id) { return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", id)); }
    private Vehiculo findVehiculo(Long id) { return vehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id)); }
}
