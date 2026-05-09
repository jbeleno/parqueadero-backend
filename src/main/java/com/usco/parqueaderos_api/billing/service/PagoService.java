package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.PagoDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<PagoDTO> findAll() {
        List<Pago> base;
        if (currentUser.isSuperAdmin()) {
            base = pagoRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = pagoRepository.findByFacturaParqueaderoEmpresaId(empresaId);
        } else {
            base = pagoRepository.findByFacturaVehiculoPersonaId(currentUser.getCurrentPersonaId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagoDTO findById(Long id) {
        Pago p = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        Factura f = p.getFactura();
        if (currentUser.isSuperAdmin()) return toDTO(p);
        if (currentUser.isAdmin()) {
            if (f != null && f.getParqueadero() != null && f.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(f.getParqueadero().getEmpresa().getId());
            }
        } else {
            // USER: pago debe estar asociado a su persona (via factura -> vehiculo -> persona)
            Long personaIdRecurso = f != null && f.getVehiculo() != null && f.getVehiculo().getPersona() != null
                    ? f.getVehiculo().getPersona().getId() : null;
            currentUser.requireOwnerOrAnyAdmin(personaIdRecurso);
        }
        return toDTO(p);
    }

    @Transactional
    public PagoDTO save(PagoDTO dto) {
        Pago entity = toEntity(dto);
        if (entity.getFechaHora() == null) entity.setFechaHora(LocalDateTime.now());
        if (entity.getEstado() == null) entity.setEstado("PENDIENTE");
        return toDTO(pagoRepository.save(entity));
    }

    @Transactional
    public PagoDTO update(Long id, PagoDTO dto) {
        Pago existing = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        existing.setMonto(dto.getMonto());
        existing.setMetodo(dto.getMetodo());
        existing.setEstado(dto.getEstado());
        if (dto.getFacturaId() != null) existing.setFactura(findFactura(dto.getFacturaId()));
        return toDTO(pagoRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        pagoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        pagoRepository.deleteById(id);
    }

    private PagoDTO toDTO(Pago e) {
        PagoDTO dto = new PagoDTO();
        dto.setId(e.getId());
        dto.setFechaHora(e.getFechaHora());
        dto.setMonto(e.getMonto());
        dto.setMetodo(e.getMetodo());
        dto.setEstado(e.getEstado());
        if (e.getFactura() != null) dto.setFacturaId(e.getFactura().getId());
        return dto;
    }

    private Pago toEntity(PagoDTO dto) {
        Pago e = new Pago();
        e.setMonto(dto.getMonto());
        e.setMetodo(dto.getMetodo());
        e.setEstado(dto.getEstado());
        if (dto.getFacturaId() != null) e.setFactura(findFactura(dto.getFacturaId()));
        return e;
    }

    private Factura findFactura(Long id) { return facturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Factura", id)); }
}
