package com.usco.parqueaderos_api.report.universal;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Define un tipo de reporte universal. Cada recurso (tickets, pagos, ...)
 * implementa esta interfaz con: clave, titulo, columnas, query SQL parametrizada.
 *
 * El ReporteUniversalService toma un spec + filtros y produce JSON, CSV o PDF.
 */
public interface ReporteSpec {

    /** Clave de URL: "tickets", "pagos", "cajas", etc. */
    String clave();

    /** Titulo legible para encabezado de PDF/CSV: "Tickets cerrados". */
    String titulo();

    /** Columnas en orden: { "col_id_en_sql": "Label legible" }. */
    Map<String, String> columnas();

    /**
     * Devuelve filas como lista de mapas (col -> valor).
     * Implementacion usa EntityManager nativo para libertad de SQL.
     */
    List<Map<String, Object>> ejecutar(EntityManager em, Filtros filtros);

    /** Roles permitidos para consultar este reporte. */
    java.util.Set<String> rolesPermitidos();

    /** Filtros estandar. */
    record Filtros(
            Long parqueaderoId,
            Long empresaId,
            LocalDate desde,
            LocalDate hasta,
            Integer limite,
            Long usuarioFiltro
    ) {}
}
