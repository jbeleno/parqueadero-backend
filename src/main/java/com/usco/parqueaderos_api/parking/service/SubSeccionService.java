package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.SubSeccionDTO;
import com.usco.parqueaderos_api.parking.entity.Seccion;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import com.usco.parqueaderos_api.parking.repository.SeccionRepository;
import com.usco.parqueaderos_api.parking.repository.SubSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubSeccionService {

    private final SubSeccionRepository subSeccionRepository;
    private final SeccionRepository seccionRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<SubSeccionDTO> findAll() {
        return subSeccionRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubSeccionDTO findById(Long id) {
        return subSeccionRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
    }

    @Transactional
    public SubSeccionDTO save(SubSeccionDTO dto) {
        return toDTO(subSeccionRepository.save(toEntity(dto)));
    }

    @Transactional
    public SubSeccionDTO update(Long id, SubSeccionDTO dto) {
        SubSeccion existing = subSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
        existing.setNombre(dto.getNombre());
        existing.setAcronimo(dto.getAcronimo());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getSeccionId() != null) existing.setSeccion(findSeccion(dto.getSeccionId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(subSeccionRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        SubSeccion existing = subSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        subSeccionRepository.save(existing);
    }

    private SubSeccionDTO toDTO(SubSeccion e) {
        SubSeccionDTO dto = new SubSeccionDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setAcronimo(e.getAcronimo());
        dto.setDescripcion(e.getDescripcion());
        if (e.getSeccion() != null) { dto.setSeccionId(e.getSeccion().getId()); dto.setSeccionNombre(e.getSeccion().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private SubSeccion toEntity(SubSeccionDTO dto) {
        SubSeccion e = new SubSeccion();
        e.setNombre(dto.getNombre());
        e.setAcronimo(dto.getAcronimo());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getSeccionId() != null) e.setSeccion(findSeccion(dto.getSeccionId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Seccion findSeccion(Long id) { return seccionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seccion", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
}
