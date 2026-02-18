package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoPuntoParqueoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.PuntoParqueoDTO;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.parking.repository.SubSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PuntoParqueoService {

    private final PuntoParqueoRepository puntoParqueoRepository;
    private final SubSeccionRepository subSeccionRepository;
    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<PuntoParqueoDTO> findAll() {
        return puntoParqueoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PuntoParqueoDTO findById(Long id) {
        return puntoParqueoRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
    }

    @Transactional
    public PuntoParqueoDTO save(PuntoParqueoDTO dto) {
        return toDTO(puntoParqueoRepository.save(toEntity(dto)));
    }

    @Transactional
    public PuntoParqueoDTO update(Long id, PuntoParqueoDTO dto) {
        PuntoParqueo existing = puntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
        existing.setNombre(dto.getNombre());
        existing.setAcronimo(dto.getAcronimo());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getSubSeccionId() != null) existing.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        if (dto.getTipoPuntoParqueoId() != null) existing.setTipoPuntoParqueo(findTipo(dto.getTipoPuntoParqueoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(puntoParqueoRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        puntoParqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
        puntoParqueoRepository.deleteById(id);
    }

    private PuntoParqueoDTO toDTO(PuntoParqueo e) {
        PuntoParqueoDTO dto = new PuntoParqueoDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setAcronimo(e.getAcronimo());
        dto.setDescripcion(e.getDescripcion());
        if (e.getSubSeccion() != null) { dto.setSubSeccionId(e.getSubSeccion().getId()); dto.setSubSeccionNombre(e.getSubSeccion().getNombre()); }
        if (e.getTipoPuntoParqueo() != null) { dto.setTipoPuntoParqueoId(e.getTipoPuntoParqueo().getId()); dto.setTipoPuntoParqueoNombre(e.getTipoPuntoParqueo().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private PuntoParqueo toEntity(PuntoParqueoDTO dto) {
        PuntoParqueo e = new PuntoParqueo();
        e.setNombre(dto.getNombre());
        e.setAcronimo(dto.getAcronimo());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getSubSeccionId() != null) e.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        if (dto.getTipoPuntoParqueoId() != null) e.setTipoPuntoParqueo(findTipo(dto.getTipoPuntoParqueoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private SubSeccion findSubSeccion(Long id) { return subSeccionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id)); }
    private TipoPuntoParqueo findTipo(Long id) { return tipoPuntoParqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoPuntoParqueo", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
}
