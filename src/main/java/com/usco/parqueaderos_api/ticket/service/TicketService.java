package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.common.event.TicketCerradoEvent;
import com.usco.parqueaderos_api.common.event.TicketCreadoEvent;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import com.usco.parqueaderos_api.ticket.dto.TicketDTO;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional(readOnly = true)
    public List<TicketDTO> findAll() {
        return ticketRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketDTO findById(Long id) {
        return ticketRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    @Transactional
    public TicketDTO save(TicketDTO dto) {
        Ticket entity = toEntity(dto);
        if (entity.getFechaHoraEntrada() == null) entity.setFechaHoraEntrada(LocalDateTime.now());
        if (entity.getEstado() == null) entity.setEstado("EN_CURSO");
        Ticket saved = ticketRepository.save(entity);
        Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
        Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
        eventPublisher.publishEvent(new TicketCreadoEvent(this, saved.getId(), parqueaderoId, puntoId));
        return toDTO(saved);
    }

    @Transactional
    public TicketDTO update(Long id, TicketDTO dto) {
        Ticket existing = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        String estadoAnterior = existing.getEstado();
        existing.setFechaHoraSalida(dto.getFechaHoraSalida());
        existing.setEstado(dto.getEstado());
        existing.setMontoCalculado(dto.getMontoCalculado());
        if (dto.getVehiculoId() != null) existing.setVehiculo(findVehiculo(dto.getVehiculoId()));
        if (dto.getPuntoParqueoId() != null) existing.setPuntoParqueo(findPuntoParqueo(dto.getPuntoParqueoId()));
        if (dto.getTarifaId() != null) existing.setTarifa(findTarifa(dto.getTarifaId()));
        Ticket saved = ticketRepository.save(existing);
        // Publicar evento de cierre si el ticket pasa a estado CERRADO
        if (!"CERRADO".equals(estadoAnterior) && "CERRADO".equals(saved.getEstado())) {
            Long parqueaderoId = saved.getParqueadero() != null ? saved.getParqueadero().getId() : null;
            Long puntoId = saved.getPuntoParqueo() != null ? saved.getPuntoParqueo().getId() : null;
            eventPublisher.publishEvent(new TicketCerradoEvent(this, saved.getId(), parqueaderoId, puntoId));
        }
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        ticketRepository.deleteById(id);
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

    private Ticket toEntity(TicketDTO dto) {
        Ticket e = new Ticket();
        e.setFechaHoraEntrada(dto.getFechaHoraEntrada());
        e.setFechaHoraSalida(dto.getFechaHoraSalida());
        e.setEstado(dto.getEstado());
        e.setMontoCalculado(dto.getMontoCalculado());
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getVehiculoId() != null) e.setVehiculo(findVehiculo(dto.getVehiculoId()));
        if (dto.getPuntoParqueoId() != null) e.setPuntoParqueo(findPuntoParqueo(dto.getPuntoParqueoId()));
        if (dto.getTarifaId() != null) e.setTarifa(findTarifa(dto.getTarifaId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private PuntoParqueo findPuntoParqueo(Long id) { return puntoParqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id)); }
    private Vehiculo findVehiculo(Long id) { return vehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id)); }
    private Tarifa findTarifa(Long id) { return tarifaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarifa", id)); }
}
