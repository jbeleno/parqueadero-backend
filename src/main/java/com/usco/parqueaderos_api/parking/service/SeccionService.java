package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.SeccionDTO;
import com.usco.parqueaderos_api.parking.entity.Nivel;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.Seccion;
import com.usco.parqueaderos_api.parking.repository.NivelRepository;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.SeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeccionService {

    private final SeccionRepository seccionRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final NivelRepository nivelRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<SeccionDTO> findAll() {
        return seccionRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeccionDTO findById(Long id) {
        return seccionRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Seccion", id));
    }

    @Transactional
    public SeccionDTO save(SeccionDTO dto) {
        return toDTO(seccionRepository.save(toEntity(dto)));
    }

    @Transactional
    public SeccionDTO update(Long id, SeccionDTO dto) {
        Seccion existing = seccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seccion", id));
        existing.setNombre(dto.getNombre());
        existing.setAcronimo(dto.getAcronimo());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getNivelId() != null) existing.setNivel(findNivel(dto.getNivelId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(seccionRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Seccion existing = seccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seccion", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        seccionRepository.save(existing);
    }

    private SeccionDTO toDTO(Seccion e) {
        SeccionDTO dto = new SeccionDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setAcronimo(e.getAcronimo());
        dto.setDescripcion(e.getDescripcion());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getNivel() != null) { dto.setNivelId(e.getNivel().getId()); dto.setNivelNombre(e.getNivel().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private Seccion toEntity(SeccionDTO dto) {
        Seccion e = new Seccion();
        e.setNombre(dto.getNombre());
        e.setAcronimo(dto.getAcronimo());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getNivelId() != null) e.setNivel(findNivel(dto.getNivelId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private Nivel findNivel(Long id) { return nivelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nivel", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
}
