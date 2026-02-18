package com.usco.parqueaderos_api.billing.service;

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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;

    @Transactional(readOnly = true)
    public List<PagoDTO> findAll() {
        return pagoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagoDTO findById(Long id) {
        return pagoRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
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
