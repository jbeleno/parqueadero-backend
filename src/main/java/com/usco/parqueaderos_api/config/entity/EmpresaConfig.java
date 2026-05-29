package com.usco.parqueaderos_api.config.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Configuracion key-value por empresa. v49 Fase 3.
 *
 * Saca todos los strings/numeros magicos del codigo a una tabla
 * editable por ADMIN de empresa.
 *
 * El campo {@code tipo} indica como interpretar {@code valor}:
 *  - STRING:  texto literal
 *  - INTEGER: parseable a int (validar contra valorMin/valorMax)
 *  - DECIMAL: parseable a double (con valorMin/valorMax)
 *  - BOOLEAN: "true" o "false"
 *  - REGEX:   patron Java de regex
 *
 * Lookup en cascada (en services): si una empresa no tiene la clave,
 * se usa el valor seed default (que existe en TODAS las empresas
 * tras la migracion inicial).
 */
@Entity
@Table(name = "empresa_config", uniqueConstraints = {
    @UniqueConstraint(name = "uq_empresa_config_clave", columnNames = {"empresa_id", "clave"})
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 150)
    private String clave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(nullable = false, length = 20)
    private String tipo = "STRING";

    @Column(name = "valor_min")
    private BigDecimal valorMin;

    @Column(name = "valor_max")
    private BigDecimal valorMax;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    private Boolean editable = true;

    @Column(name = "actualizado_por_usuario_id")
    private Long actualizadoPorUsuarioId;
}
