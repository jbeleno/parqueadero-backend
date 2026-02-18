package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoParqueadero;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoParqueaderoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import com.usco.parqueaderos_api.location.repository.CiudadRepository;
import com.usco.parqueaderos_api.parking.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.EmpresaRepository;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParqueaderoService {

    private final ParqueaderoRepository parqueaderoRepository;
    private final CiudadRepository ciudadRepository;
    private final EmpresaRepository empresaRepository;
    private final TipoParqueaderoRepository tipoParqueaderoRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<ParqueaderoDTO> findAll() {
        return parqueaderoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParqueaderoDTO findById(Long id) {
        return parqueaderoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
    }

    @Transactional
    public ParqueaderoDTO save(ParqueaderoDTO dto) {
        Parqueadero entity = toEntity(dto);
        return toDTO(parqueaderoRepository.save(entity));
    }

    @Transactional
    public ParqueaderoDTO update(Long id, ParqueaderoDTO dto) {
        Parqueadero existing = parqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
        existing.setNombre(dto.getNombre());
        existing.setDireccion(dto.getDireccion());
        existing.setTelefono(dto.getTelefono());
        existing.setLatitud(dto.getLatitud());
        existing.setLongitud(dto.getLongitud());
        existing.setAltitud(dto.getAltitud());
        existing.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        existing.setZonaHoraria(dto.getZonaHoraria());
        existing.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        existing.setModoCobro(dto.getModoCobro());
        if (dto.getHoraInicio() != null) existing.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        if (dto.getHoraFin() != null) existing.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        if (dto.getCiudadId() != null) existing.setCiudad(findCiudad(dto.getCiudadId()));
        if (dto.getEmpresaId() != null) existing.setEmpresa(findEmpresa(dto.getEmpresaId()));
        if (dto.getTipoParqueaderoId() != null) existing.setTipoParqueadero(findTipoParqueadero(dto.getTipoParqueaderoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(parqueaderoRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Parqueadero existing = parqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        parqueaderoRepository.save(existing);
    }

    public ParqueaderoDTO toDTO(Parqueadero e) {
        ParqueaderoDTO dto = new ParqueaderoDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDireccion(e.getDireccion());
        dto.setTelefono(e.getTelefono());
        dto.setLatitud(e.getLatitud());
        dto.setLongitud(e.getLongitud());
        dto.setAltitud(e.getAltitud());
        dto.setNumeroPuntosParqueo(e.getNumeroPuntosParqueo());
        dto.setZonaHoraria(e.getZonaHoraria());
        dto.setTiempoGraciaMinutos(e.getTiempoGraciaMinutos());
        dto.setModoCobro(e.getModoCobro());
        if (e.getHoraInicio() != null) dto.setHoraInicio(e.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (e.getHoraFin() != null) dto.setHoraFin(e.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (e.getCiudad() != null) { dto.setCiudadId(e.getCiudad().getId()); dto.setCiudadNombre(e.getCiudad().getNombre()); }
        if (e.getEmpresa() != null) { dto.setEmpresaId(e.getEmpresa().getId()); dto.setEmpresaNombre(e.getEmpresa().getNombre()); }
        if (e.getTipoParqueadero() != null) { dto.setTipoParqueaderoId(e.getTipoParqueadero().getId()); dto.setTipoParqueaderoNombre(e.getTipoParqueadero().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private Parqueadero toEntity(ParqueaderoDTO dto) {
        Parqueadero e = new Parqueadero();
        e.setNombre(dto.getNombre());
        e.setDireccion(dto.getDireccion());
        e.setTelefono(dto.getTelefono());
        e.setLatitud(dto.getLatitud());
        e.setLongitud(dto.getLongitud());
        e.setAltitud(dto.getAltitud());
        e.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        e.setZonaHoraria(dto.getZonaHoraria());
        e.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        e.setModoCobro(dto.getModoCobro());
        if (dto.getHoraInicio() != null) e.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        if (dto.getHoraFin() != null) e.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        if (dto.getCiudadId() != null) e.setCiudad(findCiudad(dto.getCiudadId()));
        if (dto.getEmpresaId() != null) e.setEmpresa(findEmpresa(dto.getEmpresaId()));
        if (dto.getTipoParqueaderoId() != null) e.setTipoParqueadero(findTipoParqueadero(dto.getTipoParqueaderoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Ciudad findCiudad(Long id) {
        return ciudadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ciudad", id));
    }
    private Empresa findEmpresa(Long id) {
        return empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
    }
    private TipoParqueadero findTipoParqueadero(Long id) {
        return tipoParqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", id));
    }
    private Estado findEstado(Long id) {
        return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
}
