package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<TarifaDTO> findAll() {
        List<Tarifa> base;
        if (currentUser.isSuperAdmin() || currentUser.isUser()) {
            // USER tambien necesita ver tarifas para saber cuanto pagara
            base = tarifaRepository.findAll();
        } else {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = tarifaRepository.findByParqueaderoEmpresaId(empresaId);
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TarifaDTO findById(Long id) {
        Tarifa t = tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", id));
        if (currentUser.isAdmin() && !currentUser.isSuperAdmin()
                && t.getParqueadero() != null && t.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(t.getParqueadero().getEmpresa().getId());
        }
        return toDTO(t);
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
        aplicarCamposOpcionales(existing, dto);
        return toDTO(tarifaRepository.save(existing));
    }

    /** Aplica los campos opcionales (Modelo B, IVA, suscripciones) si vinieron en el DTO. */
    private void aplicarCamposOpcionales(Tarifa e, TarifaDTO dto) {
        if (dto.getMinutosGracia() != null) e.setMinutosGracia(dto.getMinutosGracia());
        if (dto.getValorMinimo() != null) e.setValorMinimo(dto.getValorMinimo());
        if (dto.getMinutosCubiertosPorMinimo() != null) e.setMinutosCubiertosPorMinimo(dto.getMinutosCubiertosPorMinimo());
        if (dto.getAplicaIva() != null) e.setAplicaIva(dto.getAplicaIva());
        if (dto.getIvaPorcentaje() != null) e.setIvaPorcentaje(dto.getIvaPorcentaje());
        // Mensualidad/pase: permitir setear NULL desde el DTO para "deshabilitar"
        e.setPrecioMensualidad(dto.getPrecioMensualidad());
        e.setPrecioPaseDia(dto.getPrecioPaseDia());
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
        dto.setMinutosGracia(e.getMinutosGracia());
        dto.setValorMinimo(e.getValorMinimo());
        dto.setMinutosCubiertosPorMinimo(e.getMinutosCubiertosPorMinimo());
        dto.setAplicaIva(e.getAplicaIva());
        dto.setIvaPorcentaje(e.getIvaPorcentaje());
        dto.setPrecioMensualidad(e.getPrecioMensualidad());
        dto.setPrecioPaseDia(e.getPrecioPaseDia());
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
        aplicarCamposOpcionales(e, dto);
        return e;
    }

    private Parqueadero findParqueadero(Long id) { return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id)); }
    private TipoVehiculo findTipoVehiculo(Long id) { return tipoVehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", id)); }
}
