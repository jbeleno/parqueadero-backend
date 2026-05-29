package com.usco.parqueaderos_api.catalog.global.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Catalogo global gestionado por SUPER_ADMIN. v49 Fase 1.
 */
@Entity
@Table(name = "regimen_tributario")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class RegimenTributario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "pais_codigo", length = 200)
    private String paisCodigo;

    @Column(name = "orden_display")
    private Integer ordenDisplay;

    @Column(nullable = false)
    private Boolean activo = true;
}
