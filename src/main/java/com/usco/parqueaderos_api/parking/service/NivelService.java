package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.NivelDTO;
import com.usco.parqueaderos_api.parking.entity.Nivel;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.NivelRepository;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NivelService {

    private final NivelRepository nivelRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<NivelDTO> findAll() {
        return nivelRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NivelDTO> findByParqueadero(Long parqueaderoId) {
        return nivelRepository.findByParqueaderoId(parqueaderoId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NivelDTO findById(Long id) {
        return nivelRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
    }

    @Transactional
    public NivelDTO save(NivelDTO dto) {
        return toDTO(nivelRepository.save(toEntity(dto)));
    }

    @Transactional
    public NivelDTO update(Long id, NivelDTO dto) {
        Nivel existing = nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        existing.setNombre(dto.getNombre());
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(nivelRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        nivelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        nivelRepository.deleteById(id);
    }

    private NivelDTO toDTO(Nivel e) {
        NivelDTO dto = new NivelDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private Nivel toEntity(NivelDTO dto) {
        Nivel e = new Nivel();
        e.setNombre(dto.getNombre());
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) {
        return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
    }
    private Estado findEstado(Long id) {
        return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
}
