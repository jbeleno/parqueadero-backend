package com.usco.parqueaderos_api.audit.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Catalogo de niveles de severidad para audit_log. v49 Fase 9.
 *
 * INFO (0), WARN (1), CRITICAL (2), DEBUG (-1) por defecto.
 * SUPER_ADMIN puede agregar mas niveles propios.
 */
@Entity
@Table(name = "nivel_audit_log")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class NivelAuditLog extends BaseEntity {

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

    /** Numerico para ordenar/filtrar (mayor = mas critico). */
    @Column(nullable = false)
    private Integer severidad = 0;

    @Column(nullable = false)
    private Boolean activo = true;
}
