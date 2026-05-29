package com.usco.parqueaderos_api.config.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Validacion de campo configurable por empresa. v49 Fase 4.
 *
 * Cada empresa puede definir reglas (requerido, min, max, longitud,
 * regex) para los campos de sus DTOs. El service
 * {@link com.usco.parqueaderos_api.config.service.EmpresaValidacionService}
 * resuelve y aplica las reglas en runtime.
 *
 * Combinado con las anotaciones {@code @NotNull/@Size/@Pattern} hardcoded:
 * primero se ejecutan las anotaciones (limite tecnico de la BD), despues
 * las reglas de empresa (limite de negocio configurable). Asi el front
 * recibe el error MAS ESTRICTO de los dos.
 */
@Entity
@Table(name = "empresa_validacion_campo", uniqueConstraints = {
    @UniqueConstraint(name = "uq_emp_validacion", columnNames = {"empresa_id", "entidad", "campo"})
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaValidacionCampo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 80)
    private String entidad;

    @Column(nullable = false, length = 80)
    private String campo;

    @Column(nullable = false)
    private Boolean requerido = false;

    @Column(name = "longitud_min")
    private Integer longitudMin;

    @Column(name = "longitud_max")
    private Integer longitudMax;

    @Column(name = "valor_min")
    private BigDecimal valorMin;

    @Column(name = "valor_max")
    private BigDecimal valorMax;

    @Column(columnDefinition = "TEXT")
    private String regex;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "actualizado_por_usuario_id")
    private Long actualizadoPorUsuarioId;
}
