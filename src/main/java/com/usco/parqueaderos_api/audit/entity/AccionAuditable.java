package com.usco.parqueaderos_api.audit.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Catalogo de acciones auditables. v49 Fase 9.
 *
 * Reemplaza el VARCHAR libre {@code audit_log.accion} por una FK a este
 * catalogo, permitiendo que SUPER_ADMIN agregue nuevas acciones sin
 * recompilar y que los reportes filtren por ID estable.
 *
 * La columna {@code accion} (string) sigue existiendo en audit_log para
 * compatibilidad — se llenan AMBAS al crear un registro nuevo.
 */
@Entity
@Table(name = "accion_auditable")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class AccionAuditable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "color_hex", length = 9)
    private String colorHex;

    @Column(length = 50)
    private String icono;

    @Column(name = "orden_display")
    private Integer ordenDisplay;

    @Column(nullable = false)
    private Boolean activo = true;
}
