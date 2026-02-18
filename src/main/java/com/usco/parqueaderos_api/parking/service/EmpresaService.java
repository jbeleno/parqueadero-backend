package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.EmpresaDTO;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<EmpresaDTO> findAll() {
        return empresaRepository.findByEstadoNombreNot("ARCHIVADO").stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpresaDTO findById(Long id) {
        return empresaRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
    }

    @Transactional
    public EmpresaDTO save(EmpresaDTO dto) {
        Empresa entity = toEntity(dto);
        return toDTO(empresaRepository.save(entity));
    }

    @Transactional
    public EmpresaDTO update(Long id, EmpresaDTO dto) {
        Empresa existing = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
        existing.setNombre(dto.getNombre());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoId() != null) {
            existing.setEstado(findEstado(dto.getEstadoId()));
        }
        return toDTO(empresaRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Empresa existing = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        empresaRepository.save(existing);
    }

    public EmpresaDTO toDTO(Empresa e) {
        EmpresaDTO dto = new EmpresaDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        if (e.getEstado() != null) {
            dto.setEstadoId(e.getEstado().getId());
            dto.setEstadoNombre(e.getEstado().getNombre());
        }
        return dto;
    }

    private Empresa toEntity(EmpresaDTO dto) {
        Empresa e = new Empresa();
        e.setNombre(dto.getNombre());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Estado findEstado(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
}
