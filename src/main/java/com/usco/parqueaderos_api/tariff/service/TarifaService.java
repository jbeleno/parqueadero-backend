package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.tariff.dto.TarifaDTO;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
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
    private final ParqueaderoRepository parqueaderoRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;

    @Transactional(readOnly = true)
    public List<TarifaDTO> findAll() {
        return tarifaRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TarifaDTO findById(Long id) {
        return tarifaRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", id));
    }

    @Transactional
    public TarifaDTO save(TarifaDTO dto) {
        return toDTO(tarifaRepository.save(toEntity(dto)));
    }

    @Transactional
    public TarifaDTO update(Long id, TarifaDTO dto) {
        Tarifa existing = tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", id));
        existing.setNombre(dto.getNombre());
        existing.setValor(dto.getValor());
        existing.setUnidad(dto.getUnidad());
        existing.setMinutosFraccion(dto.getMinutosFraccion());
        if (dto.getFechaInicioVigencia() != null) existing.setFechaInicioVigencia(LocalDate.parse(dto.getFechaInicioVigencia()));
        if (dto.getFechaFinVigencia() != null) existing.setFechaFinVigencia(LocalDate.parse(dto.getFechaFinVigencia()));
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getTipoVehiculoId() != null) existing.setTipoVehiculo(findTipoVehiculo(dto.getTipoVehiculoId()));
        return toDTO(tarifaRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        tarifaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarifa", id));
        tarifaRepository.deleteById(id);
    }

    private TarifaDTO toDTO(Tarifa e) {
        TarifaDTO dto = new TarifaDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setValor(e.getValor());
        dto.setUnidad(e.getUnidad());
        dto.setMinutosFraccion(e.getMinutosFraccion());
        if (e.getFechaInicioVigencia() != null) dto.setFechaInicioVigencia(e.getFechaInicioVigencia().toString());
        if (e.getFechaFinVigencia() != null) dto.setFechaFinVigencia(e.getFechaFinVigencia().toString());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getTipoVehiculo() != null) { dto.setTipoVehiculoId(e.getTipoVehiculo().getId()); dto.setTipoVehiculoNombre(e.getTipoVehiculo().getNombre()); }
        return dto;
    }

    private Tarifa toEntity(TarifaDTO dto) {
        Tarifa e = new Tarifa();
        e.setNombre(dto.getNombre());
        e.setValor(dto.getValor());
        e.setUnidad(dto.getUnidad());
        e.setMinutosFraccion(dto.getMinutosFraccion());
        if (dto.getFechaInicioVigencia() != null) e.setFechaInicioVigencia(LocalDate.parse(dto.getFechaInicioVigencia()));
        if (dto.getFechaFinVigencia() != null) e.setFechaFinVigencia(LocalDate.parse(dto.getFechaFinVigencia()));
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getTipoVehiculoId() != null) e.setTipoVehiculo(findTipoVehiculo(dto.getTipoVehiculoId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private TipoVehiculo findTipoVehiculo(Long id) { return tipoVehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", id)); }
}
