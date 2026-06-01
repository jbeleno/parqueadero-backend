package com.usco.parqueaderos_api.catalog.global.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Tabla puente: cada empresa decide cuales items canonicos de cada catalogo
 * global acepta. v50 Sprint 1.
 *
 * Si una empresa NO tiene filas para un {@code catalogo} dado, acepta TODOS
 * los globales activos por default (retrocompat).
 *
 * Si tiene 1+ filas, solo acepta los que estan marcados con activo=true.
 */
@Entity
@Table(name = "empresa_catalogo_global_activo", uniqueConstraints = {
    @UniqueConstraint(name = "uq_emp_cat_global", columnNames = {"empresa_id", "catalogo", "item_id"})
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaCatalogoGlobalActivo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    /** Nombre logico del catalogo: "tipo_documento", "genero", "moneda", etc. */
    @Column(nullable = false, length = 80)
    private String catalogo;

    /** Id del item dentro del catalogo correspondiente (tipo_documento.id, genero.id, etc.) */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "actualizado_por_usuario_id")
    private Long actualizadoPorUsuarioId;
}
