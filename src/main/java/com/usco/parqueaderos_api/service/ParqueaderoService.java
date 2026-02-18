package com.usco.parqueaderos_api.service;

import com.usco.parqueaderos_api.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.entity.*;
import com.usco.parqueaderos_api.repository.*;
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
    private final EstadoRepository estadoRepository;
    
    @Transactional(readOnly = true)
    public List<ParqueaderoDTO> findAll() {
        return parqueaderoRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ParqueaderoDTO findById(Long id) {
        return parqueaderoRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Parqueadero no encontrado"));
    }
    
    @Transactional
    public ParqueaderoDTO save(ParqueaderoDTO dto) {
        Parqueadero parqueadero = convertToEntity(dto);
        return convertToDTO(parqueaderoRepository.save(parqueadero));
    }
    
    @Transactional
    public ParqueaderoDTO update(Long id, ParqueaderoDTO dto) {
        Parqueadero parqueadero = parqueaderoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parqueadero no encontrado"));
        
        parqueadero.setNombre(dto.getNombre());
        parqueadero.setDireccion(dto.getDireccion());
        parqueadero.setTelefono(dto.getTelefono());
        parqueadero.setLatitud(dto.getLatitud());
        parqueadero.setLongitud(dto.getLongitud());
        parqueadero.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        parqueadero.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        parqueadero.setModoCobro(dto.getModoCobro());
        
        if (dto.getHoraInicio() != null) {
            parqueadero.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        }
        if (dto.getHoraFin() != null) {
            parqueadero.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        }
        
        return convertToDTO(parqueaderoRepository.save(parqueadero));
    }
    
    @Transactional
    public void delete(Long id) {
        parqueaderoRepository.deleteById(id);
    }
    
    private ParqueaderoDTO convertToDTO(Parqueadero entity) {
        ParqueaderoDTO dto = new ParqueaderoDTO();
        dto.setId(entity.getId());
        dto.setNombre(entity.getNombre());
        dto.setDireccion(entity.getDireccion());
        dto.setTelefono(entity.getTelefono());
        dto.setLatitud(entity.getLatitud());
        dto.setLongitud(entity.getLongitud());
        dto.setNumeroPuntosParqueo(entity.getNumeroPuntosParqueo());
        dto.setTiempoGraciaMinutos(entity.getTiempoGraciaMinutos());
        dto.setModoCobro(entity.getModoCobro());
        
        if (entity.getHoraInicio() != null) {
            dto.setHoraInicio(entity.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (entity.getHoraFin() != null) {
            dto.setHoraFin(entity.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        if (entity.getCiudad() != null) {
            dto.setCiudadId(entity.getCiudad().getId());
            dto.setCiudadNombre(entity.getCiudad().getNombre());
        }
        if (entity.getEmpresa() != null) {
            dto.setEmpresaId(entity.getEmpresa().getId());
            dto.setEmpresaNombre(entity.getEmpresa().getNombre());
        }
        if (entity.getTipoParqueadero() != null) {
            dto.setTipoParqueaderoId(entity.getTipoParqueadero().getId());
            dto.setTipoParqueaderoNombre(entity.getTipoParqueadero().getNombre());
        }
        if (entity.getEstado() != null) {
            dto.setEstadoId(entity.getEstado().getId());
            dto.setEstadoNombre(entity.getEstado().getNombre());
        }
        
        return dto;
    }
    
    private Parqueadero convertToEntity(ParqueaderoDTO dto) {
        Parqueadero entity = new Parqueadero();
        entity.setNombre(dto.getNombre());
        entity.setDireccion(dto.getDireccion());
        entity.setTelefono(dto.getTelefono());
        entity.setLatitud(dto.getLatitud());
        entity.setLongitud(dto.getLongitud());
        entity.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        entity.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        entity.setModoCobro(dto.getModoCobro());
        
        if (dto.getHoraInicio() != null) {
            entity.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        }
        if (dto.getHoraFin() != null) {
            entity.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        }
        
        // Aquí deberías cargar las relaciones desde los repositorios
        // Por simplicidad, asumo que el DTO tiene los IDs
        
        return entity;
    }
}
