package com.usco.parqueaderos_api.config.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.config.entity.EmpresaValidacionCampo;
import com.usco.parqueaderos_api.config.repository.EmpresaValidacionCampoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Valida valores contra reglas configurables por empresa. v49 Fase 4.
 *
 * Patron de uso desde services:
 * <pre>
 *   validacionService.validar(empresaId, "persona", "nombre", dto.getNombre());
 *   validacionService.validarNumero(empresaId, "tarifa", "valor", dto.getValor());
 * </pre>
 *
 * Lanza {@link BusinessException} con codigo "ERR_VALIDATION" y el
 * mensaje configurado en la regla (o uno generico si esta vacio).
 *
 * Cache: 'empresaValidacion' (key = empresaId:entidad:campo).
 */
@Service
@RequiredArgsConstructor
public class EmpresaValidacionService {

    private final EmpresaValidacionCampoRepository repo;

    @Transactional(readOnly = true)
    @Cacheable(value = "empresaValidacion",
               key = "#empresaId + ':' + #entidad + ':' + #campo",
               unless = "#result == null")
    public EmpresaValidacionCampo regla(Long empresaId, String entidad, String campo) {
        if (empresaId == null) return null;
        return repo.findByEmpresaIdAndEntidadAndCampo(empresaId, entidad, campo)
                .filter(r -> Boolean.TRUE.equals(r.getActiva()))
                .orElse(null);
    }

    /**
     * Valida un valor STRING contra la regla configurada. Si no hay regla
     * para ese (empresa, entidad, campo), es no-op.
     */
    public void validar(Long empresaId, String entidad, String campo, String valor) {
        EmpresaValidacionCampo regla = regla(empresaId, entidad, campo);
        if (regla == null) return;

        boolean esVacio = valor == null || valor.trim().isEmpty();
        if (Boolean.TRUE.equals(regla.getRequerido()) && esVacio) {
            throw new BusinessException(
                    msg(regla, "%s.%s es obligatorio", entidad, campo),
                    "ERR_VALIDATION");
        }
        if (esVacio) return; // resto de reglas no aplican a NULL

        if (regla.getLongitudMin() != null && valor.length() < regla.getLongitudMin()) {
            throw new BusinessException(
                    msg(regla, "%s.%s minimo %d caracteres", entidad, campo, regla.getLongitudMin()),
                    "ERR_VALIDATION");
        }
        if (regla.getLongitudMax() != null && valor.length() > regla.getLongitudMax()) {
            throw new BusinessException(
                    msg(regla, "%s.%s maximo %d caracteres", entidad, campo, regla.getLongitudMax()),
                    "ERR_VALIDATION");
        }
        if (regla.getRegex() != null && !regla.getRegex().isBlank()) {
            try {
                if (!Pattern.matches(regla.getRegex(), valor)) {
                    throw new BusinessException(
                            msg(regla, "%s.%s no cumple el formato esperado", entidad, campo),
                            "ERR_VALIDATION");
                }
            } catch (java.util.regex.PatternSyntaxException ignored) {
                // Regex mal configurada por el ADMIN: no rompemos la operacion,
                // solo skipeamos esa validacion.
            }
        }
    }

    /**
     * Valida un valor numerico contra valorMin/valorMax.
     */
    public void validarNumero(Long empresaId, String entidad, String campo, Number valor) {
        EmpresaValidacionCampo regla = regla(empresaId, entidad, campo);
        if (regla == null) return;

        if (valor == null) {
            if (Boolean.TRUE.equals(regla.getRequerido())) {
                throw new BusinessException(
                        msg(regla, "%s.%s es obligatorio", entidad, campo),
                        "ERR_VALIDATION");
            }
            return;
        }
        BigDecimal v = new BigDecimal(valor.toString());
        if (regla.getValorMin() != null && v.compareTo(regla.getValorMin()) < 0) {
            throw new BusinessException(
                    msg(regla, "%s.%s minimo %s", entidad, campo, regla.getValorMin().toPlainString()),
                    "ERR_VALIDATION");
        }
        if (regla.getValorMax() != null && v.compareTo(regla.getValorMax()) > 0) {
            throw new BusinessException(
                    msg(regla, "%s.%s maximo %s", entidad, campo, regla.getValorMax().toPlainString()),
                    "ERR_VALIDATION");
        }
    }

    @Transactional
    @CacheEvict(value = "empresaValidacion",
                key = "#regla.empresaId + ':' + #regla.entidad + ':' + #regla.campo")
    public EmpresaValidacionCampo upsert(EmpresaValidacionCampo regla, Long usuarioId) {
        EmpresaValidacionCampo existente = repo.findByEmpresaIdAndEntidadAndCampo(
                regla.getEmpresaId(), regla.getEntidad(), regla.getCampo()).orElse(null);
        if (existente != null) {
            existente.setRequerido(regla.getRequerido());
            existente.setLongitudMin(regla.getLongitudMin());
            existente.setLongitudMax(regla.getLongitudMax());
            existente.setValorMin(regla.getValorMin());
            existente.setValorMax(regla.getValorMax());
            existente.setRegex(regla.getRegex());
            existente.setMensajeError(regla.getMensajeError());
            existente.setActiva(regla.getActiva());
            existente.setActualizadoPorUsuarioId(usuarioId);
            return repo.save(existente);
        }
        regla.setActualizadoPorUsuarioId(usuarioId);
        return repo.save(regla);
    }

    private static String msg(EmpresaValidacionCampo r, String fallbackFmt, Object... args) {
        if (r.getMensajeError() != null && !r.getMensajeError().isBlank()) return r.getMensajeError();
        return String.format(fallbackFmt, args);
    }
}
