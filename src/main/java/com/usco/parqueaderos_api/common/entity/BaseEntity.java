package com.usco.parqueaderos_api.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Base universal para entidades que requieren auditoria temporal.
 *
 * Cualquier entity que herede de esta clase obtiene automaticamente:
 *  - fecha_creacion (LocalDateTime NOT NULL, llenado en @PrePersist)
 *  - fecha_actualizacion (LocalDateTime NULL, llenado en @PreUpdate)
 *
 * No se mapea a tabla propia (@MappedSuperclass): cada subclase hereda
 * las columnas directamente en SU tabla.
 *
 * Para entities que ademas requieran tracking de usuario, usar
 * {@link AuditableEntity}.
 *
 * Migracion SQL (v49 Fase 0): las columnas se crearon con DEFAULT
 * CURRENT_TIMESTAMP a nivel BD para que los registros pre-v49 queden
 * con un valor razonable. Hibernate las sobrescribe en cada save de
 * registros nuevos via @PrePersist.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.fechaCreacion == null) {
            this.fechaCreacion = now;
        }
        // En el insert inicial dejamos fecha_actualizacion = fechaCreacion para
        // que SIEMPRE haya un valor coherente (no NULL en filas vivas).
        if (this.fechaActualizacion == null) {
            this.fechaActualizacion = this.fechaCreacion;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
