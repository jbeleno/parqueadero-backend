package com.usco.parqueaderos_api.catalog.global.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Catalogo global gestionado por SUPER_ADMIN. v49 Fase 1 (completar).
 */
@Entity
@Table(name = "canal_origen_reserva")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CanalOrigenReserva extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @jakarta.persistence.Column(length = 50)
    private String icono;

    @Column(name = "orden_display")
    private Integer ordenDisplay;

    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * v50: Si NULL → item canónico global (visible para todas las empresas).
     * Si NOT NULL → item custom propio de esa empresa.
     */
    @Column(name = "empresa_id")
    private Long empresaId;
}
