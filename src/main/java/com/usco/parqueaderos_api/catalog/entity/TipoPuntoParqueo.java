package com.usco.parqueaderos_api.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipo_punto_parqueo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class TipoPuntoParqueo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    /** Codigo corto identificable (ACTIVO, MOTO, etc). v49 Fase 6. */
    @Column(length = 50)
    private String codigo;

    /** Color hex para UI (#1e90ff). v49 Fase 6. */
    @Column(name = "color_hex", length = 9)
    private String colorHex;

    /** Nombre del icono (lucide/material). v49 Fase 6. */
    @Column(length = 50)
    private String icono;

    /** Orden de despliegue en selects (0 = primero). v49 Fase 6. */
    @Column(name = "orden_display")
    private Integer ordenDisplay;

    /** Permite desactivar sin borrar. Default true. v49 Fase 6. */
    @Column(nullable = false)
    private Boolean activo = true;
}
