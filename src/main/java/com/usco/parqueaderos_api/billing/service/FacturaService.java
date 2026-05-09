package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.FacturaDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final TicketRepository ticketRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<FacturaDTO> findAll() {
        List<Factura> base;
        if (currentUser.isSuperAdmin()) {
            base = facturaRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = facturaRepository.findByParqueaderoEmpresaId(empresaId);
        } else {
            // USER: solo facturas de sus propios vehiculos
            base = facturaRepository.findByVehiculoPersonaId(currentUser.getCurrentPersonaId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacturaDTO findById(Long id) {
        return facturaRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
    }

    @Transactional
    public FacturaDTO save(FacturaDTO dto) {
        Factura entity = toEntity(dto);
        if (entity.getFechaHora() == null) entity.setFechaHora(LocalDateTime.now());
        if (entity.getEstado() == null) entity.setEstado("PENDIENTE");
        return toDTO(facturaRepository.save(entity));
    }

    @Transactional
    public FacturaDTO update(Long id, FacturaDTO dto) {
        Factura existing = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
        existing.setValorTotal(dto.getValorTotal());
        existing.setEstado(dto.getEstado());
        return toDTO(facturaRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        facturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Factura", id));
        facturaRepository.deleteById(id);
    }

    private FacturaDTO toDTO(Factura e) {
        FacturaDTO dto = new FacturaDTO();
        dto.setId(e.getId());
        dto.setFechaHora(e.getFechaHora());
        dto.setValorTotal(e.getValorTotal());
        dto.setEstado(e.getEstado());
        if (e.getTicket() != null) dto.setTicketId(e.getTicket().getId());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getVehiculo() != null) { dto.setVehiculoId(e.getVehiculo().getId()); dto.setVehiculoPlaca(e.getVehiculo().getPlaca()); }
        return dto;
    }

    private Factura toEntity(FacturaDTO dto) {
        Factura e = new Factura();
        e.setValorTotal(dto.getValorTotal());
        e.setEstado(dto.getEstado());
        if (dto.getTicketId() != null) e.setTicket(findTicket(dto.getTicketId()));
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getVehiculoId() != null) e.setVehiculo(findVehiculo(dto.getVehiculoId()));
        return e;
    }

    private Ticket findTicket(Long id) { return ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id)); }
    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private Vehiculo findVehiculo(Long id) { return vehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id)); }
}
