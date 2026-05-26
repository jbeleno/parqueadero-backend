package com.usco.parqueaderos_api.report.universal;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Endpoints universales de reportes:
 *
 *   GET /api/reportes/listado/catalogo
 *       -> lista de reportes disponibles para el usuario actual
 *
 *   GET /api/reportes/listado/{recurso}?formato=json|csv|pdf
 *                                       &parqueaderoId=&empresaId=
 *                                       &desde=YYYY-MM-DD&hasta=YYYY-MM-DD
 *                                       &limite=5000
 *       -> reporte en el formato pedido (default json)
 *
 * RBAC por recurso (declarado en cada ReporteSpec).
 * Filtros multi-tenant forzados: ADMIN->su empresa, ADMIN_PARQ/OPERARIO->parq asignado.
 * Cada exportacion queda registrada en audit_log.
 */
@RestController
@RequestMapping("/api/reportes/listado")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO','OPERARIO_CAJA')")
public class ReporteUniversalController {

    private final ReporteUniversalService service;

    @GetMapping("/catalogo")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> catalogo() {
        return ResponseEntity.ok(ApiResponse.ok(service.catalogo()));
    }

    @GetMapping("/{recurso}")
    public ResponseEntity<?> generar(
            @PathVariable String recurso,
            @RequestParam(defaultValue = "json") String formato,
            @RequestParam(required = false) Long parqueaderoId,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) Long usuarioFiltro
    ) {
        ReporteSpec.Filtros filtros = new ReporteSpec.Filtros(
                parqueaderoId, empresaId, desde, hasta, limite, usuarioFiltro);

        if ("json".equalsIgnoreCase(formato)) {
            List<Map<String, Object>> rows = service.generarJson(recurso, filtros);
            return ResponseEntity.ok(ApiResponse.ok(rows));
        }

        ReporteUniversalService.Formato f;
        try {
            f = ReporteUniversalService.Formato.valueOf(formato.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("formato invalido: " + formato + " (usar json|csv|pdf)",
                            "ERR_FORMATO_INVALIDO"));
        }
        ReporteUniversalService.Resultado res = service.generar(recurso, filtros, f);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + res.filename() + "\"")
                .contentType(MediaType.parseMediaType(res.contentType()))
                .header("X-Reporte-Filas", String.valueOf(res.filas()))
                .body(res.bytes());
    }
}
