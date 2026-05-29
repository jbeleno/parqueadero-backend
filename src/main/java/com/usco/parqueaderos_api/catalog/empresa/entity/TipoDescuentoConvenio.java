package com.usco.parqueaderos_api.catalog.empresa.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Catalogo gestionado por ADMIN de cada empresa. v49 Fase 2.
 */
@Entity
@Table(name = "tipo_descuento_convenio", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tipodescuentoconvenio", columnNames = {"empresa_id", "codigo"})
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class TipoDescuentoConvenio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 50)
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
