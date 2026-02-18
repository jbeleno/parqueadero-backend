package com.usco.parqueaderos_api.device.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoDispositivo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoDispositivoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.device.dto.DispositivoDTO;
import com.usco.parqueaderos_api.device.entity.Dispositivo;
import com.usco.parqueaderos_api.device.repository.DispositivoRepository;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.SubSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final TipoDispositivoRepository tipoDispositivoRepository;
    private final EstadoRepository estadoRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final SubSeccionRepository subSeccionRepository;

    @Transactional(readOnly = true)
    public List<DispositivoDTO> findAll() {
        return dispositivoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DispositivoDTO findById(Long id) {
        return dispositivoRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo", id));
    }

    @Transactional
    public DispositivoDTO save(DispositivoDTO dto) {
        return toDTO(dispositivoRepository.save(toEntity(dto)));
    }

    @Transactional
    public DispositivoDTO update(Long id, DispositivoDTO dto) {
        Dispositivo existing = dispositivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo", id));
        existing.setNombre(dto.getNombre());
        existing.setUrlEndpoint(dto.getUrlEndpoint());
        if (dto.getTipoDispositivoId() != null) existing.setTipoDispositivo(findTipo(dto.getTipoDispositivoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getSubSeccionId() != null) existing.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        return toDTO(dispositivoRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        dispositivoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dispositivo", id));
        dispositivoRepository.deleteById(id);
    }

    private DispositivoDTO toDTO(Dispositivo e) {
        DispositivoDTO dto = new DispositivoDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setUrlEndpoint(e.getUrlEndpoint());
        if (e.getTipoDispositivo() != null) { dto.setTipoDispositivoId(e.getTipoDispositivo().getId()); dto.setTipoDispositivoNombre(e.getTipoDispositivo().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getSubSeccion() != null) { dto.setSubSeccionId(e.getSubSeccion().getId()); dto.setSubSeccionNombre(e.getSubSeccion().getNombre()); }
        return dto;
    }

    private Dispositivo toEntity(DispositivoDTO dto) {
        Dispositivo e = new Dispositivo();
        e.setNombre(dto.getNombre());
        e.setUrlEndpoint(dto.getUrlEndpoint());
        if (dto.getTipoDispositivoId() != null) e.setTipoDispositivo(findTipo(dto.getTipoDispositivoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getSubSeccionId() != null) e.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        return e;
    }

    private TipoDispositivo findTipo(Long id) { return tipoDispositivoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoDispositivo", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private SubSeccion findSubSeccion(Long id) { return subSeccionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id)); }
}
