package com.usco.parqueaderos_api.reservation.service;

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

    @Transactional(readOnly = true)
    public List<ReservaDTO> findAll() {
        return reservaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservaDTO findById(Long id) {
        return reservaRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
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
        existing.setFechaHoraInicio(dto.getFechaHoraInicio());
        existing.setFechaHoraFin(dto.getFechaHoraFin());
        existing.setEstado(dto.getEstado());
        if (dto.getUsuarioId() != null) existing.setUsuario(findUsuario(dto.getUsuarioId()));
        if (dto.getVehiculoId() != null) existing.setVehiculo(findVehiculo(dto.getVehiculoId()));
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getPuntoParqueoId() != null) existing.setPuntoParqueo(findPuntoParqueo(dto.getPuntoParqueoId()));
        return toDTO(reservaRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        reservaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva", id));
        reservaRepository.deleteById(id);
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
