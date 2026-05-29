package com.usco.parqueaderos_api.common.validation;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.config.service.EmpresaConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Centraliza la validacion del campo "motivo" usado en anulaciones,
 * archivados, ajustes manuales, etc. v49 Fase 11.
 *
 * Antes: cada service hardcodeaba `motivo.length() < 10` con el
 * mismo "min 10 chars" repetido. Ahora la regla vive en
 * {@code empresa_config} clave {@code motivo.min_chars} (default 10) y
 * {@code motivo.max_chars} (default 500), editable por ADMIN.
 *
 * Si la empresaId es null (operacion super-admin sin contexto), cae
 * al default codificado (10/500).
 */
@Component
@RequiredArgsConstructor
public class MotivoValidator {

    /** Defaults seed; iguales a los valores de empresa_config para nuevas empresas. */
    public static final int DEFAULT_MIN_CHARS = 10;
    public static final int DEFAULT_MAX_CHARS = 500;

    @Autowired(required = false)
    private EmpresaConfigService configService;

    /**
     * Valida el motivo. Lanza BusinessException con codigo ERR_MOTIVO_INVALIDO
     * si no cumple. Si el motivo es null/vacio, tambien falla.
     *
     * @param empresaId opcional — null usa defaults
     * @param motivo el texto entrado por el usuario
     */
    public void validar(Long empresaId, String motivo) {
        int minChars = (configService != null && empresaId != null)
                ? configService.getInt(empresaId, "motivo.min_chars", DEFAULT_MIN_CHARS)
                : DEFAULT_MIN_CHARS;
        int maxChars = (configService != null && empresaId != null)
                ? configService.getInt(empresaId, "motivo.max_chars", DEFAULT_MAX_CHARS)
                : DEFAULT_MAX_CHARS;

        if (motivo == null || motivo.trim().length() < minChars) {
            throw new BusinessException(
                    "El motivo es obligatorio (minimo " + minChars + " caracteres)",
                    "ERR_MOTIVO_INVALIDO");
        }
        if (motivo.length() > maxChars) {
            throw new BusinessException(
                    "El motivo excede el maximo (" + maxChars + " caracteres)",
                    "ERR_MOTIVO_INVALIDO");
        }
    }
}
