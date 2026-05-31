package com.usco.parqueaderos_api.common.util;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.validation.MotivoValidator;

import java.time.LocalDateTime;

/**
 * Helper para soft-delete uniforme. v49 Fase 10.
 *
 * Toda entidad de negocio que herede {@code BaseEntity} y tenga las
 * columnas {@code archivado_en} + {@code archivado_por_usuario_id}
 * (agregadas en v49 Fase 10 a 12 tablas) puede usar este helper para
 * archivado consistente:
 *
 * <pre>
 *   public void archivar(Long id, String motivo) {
 *       Empresa e = empresaRepo.findById(id).orElseThrow(...);
 *       SoftDeleteHelper.archivar(e, currentUser.getCurrentUserId(), motivo,
 *           Empresa::setArchivadoEn, Empresa::setArchivadoPorUsuarioId);
 *       empresaRepo.save(e);
 *   }
 * </pre>
 *
 * Garantiza:
 * - motivo no nulo con longitud minima
 * - archivado_en = ahora
 * - archivado_por_usuario_id = usuario que ejecuta
 * - throw BusinessException si ya estaba archivado
 */
public final class SoftDeleteHelper {

    private SoftDeleteHelper() {}

    /**
     * Marca la entidad como archivada. Lanza si ya lo estaba o si motivo es invalido.
     */
    public static <T> void archivar(
            T entity,
            Long usuarioId,
            String motivo,
            java.util.function.BiConsumer<T, LocalDateTime> setArchivadoEn,
            java.util.function.BiConsumer<T, Long> setArchivadoPorUsuarioId,
            java.util.function.Function<T, LocalDateTime> getArchivadoEn) {

        if (entity == null) {
            throw new BusinessException("La entidad a archivar no puede ser null", "ERR_INVALID_STATE");
        }
        LocalDateTime ya = getArchivadoEn.apply(entity);
        if (ya != null) {
            throw new BusinessException("La entidad ya estaba archivada en " + ya, "ERR_ALREADY_ARCHIVED");
        }
        if (motivo == null || motivo.trim().length() < MotivoValidator.DEFAULT_MIN_CHARS) {
            throw new BusinessException(
                    "Motivo de archivado obligatorio (min " + MotivoValidator.DEFAULT_MIN_CHARS + " caracteres)",
                    "ERR_MOTIVO_INVALIDO");
        }
        setArchivadoEn.accept(entity, LocalDateTime.now());
        setArchivadoPorUsuarioId.accept(entity, usuarioId);
    }

    /**
     * Restaura una entidad archivada (poner archivado_en = NULL).
     */
    public static <T> void restaurar(
            T entity,
            java.util.function.BiConsumer<T, LocalDateTime> setArchivadoEn,
            java.util.function.BiConsumer<T, Long> setArchivadoPorUsuarioId,
            java.util.function.Function<T, LocalDateTime> getArchivadoEn) {

        if (getArchivadoEn.apply(entity) == null) {
            throw new BusinessException("La entidad no estaba archivada", "ERR_NOT_ARCHIVED");
        }
        setArchivadoEn.accept(entity, null);
        setArchivadoPorUsuarioId.accept(entity, null);
    }
}
