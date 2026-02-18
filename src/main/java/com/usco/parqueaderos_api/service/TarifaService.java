package com.usco.parqueaderos_api.service;

import com.usco.parqueaderos_api.dto.TarifaDTO;
import com.usco.parqueaderos_api.entity.Tarifa;
import com.usco.parqueaderos_api.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifaService {
    
    private final TarifaRepository tarifaRepository;
    
    @Transactional(readOnly = true)
    public List<TarifaDTO> findAll() {
        return tarifaRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TarifaDTO findById(Long id) {
        return tarifaRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
    }
    
    @Transactional
    public TarifaDTO save(TarifaDTO dto) {
        Tarifa tarifa = convertToEntity(dto);
        return convertToDTO(tarifaRepository.save(tarifa));
    }
    
    @Transactional
    public TarifaDTO update(Long id, TarifaDTO dto) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
        
        tarifa.setNombre(dto.getNombre());
        tarifa.setValor(dto.getValor());
        tarifa.setUnidad(dto.getUnidad());
        tarifa.setMinutosFraccion(dto.getMinutosFraccion());
        
        if (dto.getFechaInicioVigencia() != null) {
            tarifa.setFechaInicioVigencia(LocalDate.parse(dto.getFechaInicioVigencia()));
        }
        if (dto.getFechaFinVigencia() != null) {
            tarifa.setFechaFinVigencia(LocalDate.parse(dto.getFechaFinVigencia()));
        }
        
        return convertToDTO(tarifaRepository.save(tarifa));
    }
    
    @Transactional
    public void delete(Long id) {
        tarifaRepository.deleteById(id);
    }
    
    private TarifaDTO convertToDTO(Tarifa entity) {
        TarifaDTO dto = new TarifaDTO();
        dto.setId(entity.getId());
        dto.setNombre(entity.getNombre());
        dto.setValor(entity.getValor());
        dto.setUnidad(entity.getUnidad());
        dto.setMinutosFraccion(entity.getMinutosFraccion());
        
        if (entity.getFechaInicioVigencia() != null) {
            dto.setFechaInicioVigencia(entity.getFechaInicioVigencia().toString());
        }
        if (entity.getFechaFinVigencia() != null) {
            dto.setFechaFinVigencia(entity.getFechaFinVigencia().toString());
        }
        
        if (entity.getParqueadero() != null) {
            dto.setParqueaderoId(entity.getParqueadero().getId());
            dto.setParqueaderoNombre(entity.getParqueadero().getNombre());
        }
        if (entity.getTipoVehiculo() != null) {
            dto.setTipoVehiculoId(entity.getTipoVehiculo().getId());
            dto.setTipoVehiculoNombre(entity.getTipoVehiculo().getNombre());
        }
        
        return dto;
    }
    
    private Tarifa convertToEntity(TarifaDTO dto) {
        Tarifa entity = new Tarifa();
        entity.setNombre(dto.getNombre());
        entity.setValor(dto.getValor());
        entity.setUnidad(dto.getUnidad());
        entity.setMinutosFraccion(dto.getMinutosFraccion());
        
        if (dto.getFechaInicioVigencia() != null) {
            entity.setFechaInicioVigencia(LocalDate.parse(dto.getFechaInicioVigencia()));
        }
        if (dto.getFechaFinVigencia() != null) {
            entity.setFechaFinVigencia(LocalDate.parse(dto.getFechaFinVigencia()));
        }
        
        return entity;
    }
}
