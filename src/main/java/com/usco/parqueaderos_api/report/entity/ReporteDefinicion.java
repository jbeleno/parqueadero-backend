package com.usco.parqueaderos_api.report.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Definicion de un reporte parametrizable. v49 Fase 8.
 *
 * Reemplaza ReportesSpecs.java hardcoded: ahora cualquier SUPER_ADMIN
 * puede agregar reportes via INSERT (o endpoint futuro) sin recompilar.
 *
 * empresa_id NULL = reporte GLOBAL (disponible para todas las empresas).
 * empresa_id NOT NULL = reporte custom de UNA empresa.
 *
 * sql_template usa parametros nombrados :nombre. El service los
 * resuelve via NamedParameterJdbcTemplate (anti-SQL-injection).
 */
@Entity
@Table(name = "reporte_definicion")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDefinicion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL = reporte global. */
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false, length = 80)
    private String clave;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "sql_template", columnDefinition = "TEXT", nullable = false)
    private String sqlTemplate;

    @Column(name = "filtros_json", columnDefinition = "TEXT")
    private String filtrosJson;

    @Column(name = "columnas_json", columnDefinition = "TEXT")
    private String columnasJson;

    @Column(name = "roles_permitidos", length = 300)
    private String rolesPermitidos;

    @Column(name = "max_filas", nullable = false)
    private Integer maxFilas = 5000;

    @Column(name = "formato_default", nullable = false, length = 10)
    private String formatoDefault = "JSON";

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "actualizado_por_usuario_id")
    private Long actualizadoPorUsuarioId;
}
