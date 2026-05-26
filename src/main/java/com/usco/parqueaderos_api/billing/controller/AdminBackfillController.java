package com.usco.parqueaderos_api.billing.controller;

import com.usco.parqueaderos_api.billing.service.BackfillFacturaService;
import com.usco.parqueaderos_api.billing.service.BackfillFacturaService.BackfillResult;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints admin para mantenimiento / backfill. Solo SUPER_ADMIN.
 * Acciones reversibles: cada factura creada por backfill lleva el campo
 * origen = "BACKFILL_<timestamp>" para poder revertir con
 * DELETE FROM factura WHERE origen LIKE 'BACKFILL_%';
 */
@RestController
@RequestMapping("/api/admin/backfill")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminBackfillController {

    private final BackfillFacturaService backfillService;

    /**
     * Lista tickets candidatos a generar factura (CERRADOS con monto>0 sin
     * factura) en una ventana de fechas. No crea nada — solo proyecta.
     * Si parqueaderoId es null, considera todos.
     */
    @GetMapping("/facturas-faltantes")
    public ResponseEntity<ApiResponse<BackfillResult>> dryRun(
            @RequestParam(required = false) Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(
                backfillService.ejecutar(parqueaderoId, desde, hasta, true)));
    }

    /**
     * Aplica el backfill: crea las facturas para los tickets candidatos.
     * Cada factura queda marcada con origen=BACKFILL_<timestamp>.
     * Reversible con: DELETE FROM factura WHERE origen LIKE 'BACKFILL_%';
     */
    @PostMapping("/facturas-faltantes")
    public ResponseEntity<ApiResponse<BackfillResult>> aplicar(
            @RequestParam(required = false) Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(
                backfillService.ejecutar(parqueaderoId, desde, hasta, false)));
    }
}
