package com.usco.parqueaderos_api.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Extiende {@link BaseEntity} agregando tracking de USUARIO. Util para
 * entidades de negocio donde importa saber QUIEN creo y QUIEN actualizo,
 * no solo cuando.
 *
 * El llenado de creadoPorUsuarioId / actualizadoPorUsuarioId se hace
 * MANUAL en los services (no hay listener AOP universal porque algunos
 * flujos —jobs, OCR, backfills— no tienen usuario humano).
 *
 * Patron recomendado en services:
 *
 * <pre>
 *   if (currentUser.getCurrentUserId() != null) {
 *       if (entity.getId() == null) entity.setCreadoPorUsuarioId(uid);
 *       entity.setActualizadoPorUsuarioId(uid);
 *   }
 * </pre>
 *
 * Para flujos sin usuario (jobs, OCR, listeners), dejar NULL.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "creado_por_usuario_id", updatable = false)
    private Long creadoPorUsuarioId;

    @Column(name = "actualizado_por_usuario_id")
    private Long actualizadoPorUsuarioId;
}
