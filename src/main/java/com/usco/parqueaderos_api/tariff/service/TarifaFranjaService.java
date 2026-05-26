package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.tariff.dto.TarifaFranjaDTO;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.entity.TarifaFranja;
import com.usco.parqueaderos_api.tariff.repository.TarifaFranjaRepository;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifaFranjaService {

    private final TarifaFranjaRepository franjaRepository;
    private final TarifaRepository tarifaRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<TarifaFranjaDTO> findByTarifa(Long tarifaId) {
        Tarifa t = requireTarifaAccesible(tarifaId);
        return franjaRepository.findByTarifaId(t.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TarifaFranjaDTO save(TarifaFranjaDTO dto) {
        requireAdmin();
        if (dto.getTarifaId() == null) {
            throw new BusinessException("tarifaId es obligatorio", "ERR_MISSING_FIELDS");
        }
        Tarifa t = requireTarifaAccesible(dto.getTarifaId());
        if (dto.getHoraInicio() == null || dto.getHoraFin() == null) {
            throw new BusinessException("horaInicio y horaFin son obligatorios", "ERR_MISSING_FIELDS");
        }
        if (dto.getValor() == null || dto.getValor() <= 0) {
            throw new BusinessException("valor debe ser mayor a cero", "ERR_INVALID_VALUE");
        }
        TarifaFranja f = new TarifaFranja();
        f.setTarifa(t);
        f.setNombre(dto.getNombre());
        f.setHoraInicio(dto.getHoraInicio());
        f.setHoraFin(dto.getHoraFin());
        f.setValor(dto.getValor());
        f.setSoloFinesDeSemana(dto.getSoloFinesDeSemana() != null && dto.getSoloFinesDeSemana());
        f.setActiva(dto.getActiva() == null || dto.getActiva());
        return toDTO(franjaRepository.save(f));
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        TarifaFranja f = franjaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarifaFranja", id));
        requireTarifaAccesible(f.getTarifa().getId());
        franjaRepository.deleteById(id);
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN");
        }
    }

    private Tarifa requireTarifaAccesible(Long tarifaId) {
        Tarifa t = tarifaRepository.findById(tarifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", tarifaId));
        if (!currentUser.isSuperAdmin() && t.getParqueadero() != null
                && t.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(t.getParqueadero().getEmpresa().getId());
        }
        return t;
    }

    private TarifaFranjaDTO toDTO(TarifaFranja f) {
        return new TarifaFranjaDTO(
                f.getId(),
                f.getTarifa() != null ? f.getTarifa().getId() : null,
                f.getNombre(),
                f.getHoraInicio(),
                f.getHoraFin(),
                f.getValor(),
                f.getSoloFinesDeSemana(),
                f.getActiva()
        );
    }
}
