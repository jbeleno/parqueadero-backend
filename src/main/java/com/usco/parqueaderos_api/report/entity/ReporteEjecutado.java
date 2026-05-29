package com.usco.parqueaderos_api.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Historial de ejecuciones de reportes. v49 Fase 8.
 *
 * Cada vez que un usuario ejecuta un reporte, se registra aqui con
 * sus parametros, duracion y resultado. Util para:
 *  - Auditoria de quien ejecuto que
 *  - Detectar reportes lentos (sort by duracion_ms)
 *  - Detectar errores recurrentes
 */
@Entity
@Table(name = "reporte_ejecutado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEjecutado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporte_definicion_id")
    private Long reporteDefinicionId;

    @Column(name = "clave_reporte", nullable = false, length = 80)
    private String claveReporte;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "parqueadero_id")
    private Long parqueaderoId;

    @Column(name = "parametros_json", columnDefinition = "TEXT")
    private String parametrosJson;

    @Column(nullable = false, length = 10)
    private String formato = "JSON";

    @Column(name = "filas_devueltas")
    private Integer filasDevueltas;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(nullable = false, length = 20)
    private String estado = "OK";

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "ejecutado_por_usuario_id")
    private Long ejecutadoPorUsuarioId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void prePersist() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
