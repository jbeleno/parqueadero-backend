package com.usco.parqueaderos_api.convenio.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.convenio.dto.ConvenioDTO;
import com.usco.parqueaderos_api.convenio.entity.Convenio;
import com.usco.parqueaderos_api.convenio.repository.ConvenioRepository;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConvenioService {

    private static final Set<String> TIPOS_VALIDOS =
            Set.of("MONTO_FIJO", "PORCENTAJE", "MINUTOS_GRATIS");

    private final ConvenioRepository repository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<ConvenioDTO> findByParqueadero(Long parqueaderoId) {
        return repository.findByParqueaderoId(parqueaderoId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ConvenioDTO save(ConvenioDTO dto) {
        requireAdmin();
        if (dto.getParqueaderoId() == null) {
            throw new BusinessException("parqueaderoId es obligatorio", "ERR_MISSING_FIELDS");
        }
        if (dto.getTipoDescuento() == null
                || !TIPOS_VALIDOS.contains(dto.getTipoDescuento().toUpperCase())) {
            throw new BusinessException(
                    "tipoDescuento debe ser MONTO_FIJO | PORCENTAJE | MINUTOS_GRATIS",
                    "ERR_INVALID_TIPO_DESCUENTO");
        }
        validarConsistenciaSegunTipo(dto);
        Parqueadero p = parqueaderoRepository.findById(dto.getParqueaderoId())
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", dto.getParqueaderoId()));
        if (!currentUser.isSuperAdmin() && p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }

        Convenio c = new Convenio();
        c.setParqueadero(p);
        c.setNombreComercio(dto.getNombreComercio());
        c.setNitComercio(dto.getNitComercio());
        c.setTipoDescuento(dto.getTipoDescuento().toUpperCase());
        c.setValorDescuento(dto.getValorDescuento());
        c.setPorcentajeDescuento(dto.getPorcentajeDescuento());
        c.setMinutosGratis(dto.getMinutosGratis());
        c.setMontoMinimoCompra(dto.getMontoMinimoCompra());
        c.setFechaInicioVigencia(dto.getFechaInicioVigencia());
        c.setFechaFinVigencia(dto.getFechaFinVigencia());
        c.setActivo(dto.getActivo() == null || dto.getActivo());
        return toDTO(repository.save(c));
    }

    @Transactional
    public ConvenioDTO desactivar(Long id) {
        requireAdmin();
        Convenio c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convenio", id));
        if (!currentUser.isSuperAdmin() && c.getParqueadero() != null
                && c.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(c.getParqueadero().getEmpresa().getId());
        }
        c.setActivo(false);
        return toDTO(repository.save(c));
    }

    private void validarConsistenciaSegunTipo(ConvenioDTO dto) {
        String t = dto.getTipoDescuento().toUpperCase();
        switch (t) {
            case "MONTO_FIJO":
                if (dto.getValorDescuento() == null || dto.getValorDescuento() <= 0)
                    throw new BusinessException("valorDescuento > 0 obligatorio para MONTO_FIJO", "ERR_INVALID_VALUE");
                break;
            case "PORCENTAJE":
                if (dto.getPorcentajeDescuento() == null
                        || dto.getPorcentajeDescuento() <= 0
                        || dto.getPorcentajeDescuento() > 100)
                    throw new BusinessException("porcentajeDescuento entre 1 y 100", "ERR_INVALID_VALUE");
                break;
            case "MINUTOS_GRATIS":
                if (dto.getMinutosGratis() == null || dto.getMinutosGratis() <= 0)
                    throw new BusinessException("minutosGratis > 0 obligatorio", "ERR_INVALID_VALUE");
                break;
        }
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin())
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN");
    }

    private ConvenioDTO toDTO(Convenio c) {
        return new ConvenioDTO(
                c.getId(),
                c.getParqueadero() != null ? c.getParqueadero().getId() : null,
                c.getNombreComercio(),
                c.getNitComercio(),
                c.getTipoDescuento(),
                c.getValorDescuento(),
                c.getPorcentajeDescuento(),
                c.getMinutosGratis(),
                c.getMontoMinimoCompra(),
                c.getFechaInicioVigencia(),
                c.getFechaFinVigencia(),
                c.getActivo()
        );
    }
}
