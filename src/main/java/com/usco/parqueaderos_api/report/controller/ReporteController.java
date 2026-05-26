package com.usco.parqueaderos_api.report.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.report.dto.*;
import com.usco.parqueaderos_api.report.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints de reporting / dashboard. Requieren ADMIN o SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class ReporteController {

    private final ReporteService service;
    private final com.usco.parqueaderos_api.report.service.ReporteOperacionService operacionService;
    private final com.usco.parqueaderos_api.report.service.CierreDiaService cierreDiaService;

    /** Facturacion en un rango, agrupada por dia/semana/mes. */
    @GetMapping("/facturacion")
    public ResponseEntity<ApiResponse<ResumenFacturacionDTO>> facturacion(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "dia") String agruparPor) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.facturacion(parqueaderoId, desde, hasta, agruparPor)));
    }

    /** Facturacion segmentada por tipo de vehiculo. */
    @GetMapping("/facturacion/por-tipo-vehiculo")
    public ResponseEntity<ApiResponse<List<BucketFacturacionDTO>>> porTipoVehiculo(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.porTipoVehiculo(parqueaderoId, desde, hasta)));
    }

    /** Facturacion segregada por fuente: TICKET_NORMAL vs SUSCRIPCION. */
    @GetMapping("/facturacion/por-fuente")
    public ResponseEntity<ApiResponse<List<BucketFacturacionDTO>>> porFuente(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.porFuente(parqueaderoId, desde, hasta)));
    }

    /** Top vehiculos por visitas / facturacion. */
    @GetMapping("/top-vehiculos")
    public ResponseEntity<ApiResponse<List<TopVehiculoDTO>>> topVehiculos(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.topVehiculos(parqueaderoId, desde, hasta, limit)));
    }

    /** Comparativa mes actual vs mes anterior. */
    @GetMapping("/comparativa")
    public ResponseEntity<ApiResponse<ComparativaPeriodoDTO>> comparativa(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaReferencia) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.comparativaMensual(parqueaderoId, fechaReferencia)));
    }

    /** Resumen de suscripciones por parqueadero. */
    @GetMapping("/suscripciones/resumen")
    public ResponseEntity<ApiResponse<ResumenSuscripcionesDTO>> resumenSuscripciones(
            @RequestParam Long parqueaderoId) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.resumenSuscripciones(parqueaderoId)));
    }

    /** Percentiles de duracion de estadia. */
    @GetMapping("/operacion/duracion")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> duracion(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(
                operacionService.duracionEstadia(parqueaderoId, desde, hasta)));
    }

    /** Tickets agrupados por hora del dia para detectar picos. */
    @GetMapping("/operacion/ocupacion-horaria")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> ocupacionHoraria(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.ok(
                operacionService.ocupacionHoraria(parqueaderoId, fecha)));
    }

    /** Cierre de caja diario (existente). Si no hay, devuelve 404. */
    @GetMapping("/cierre-dia")
    public ResponseEntity<ApiResponse<com.usco.parqueaderos_api.report.dto.CierreDiaDTO>> cierreDia(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.ok(cierreDiaService.obtener(parqueaderoId, fecha)));
    }

    /**
     * Genera cierre del dia manualmente.
     * - Por defecto idempotente: si ya existe no lo recrea.
     * - Si forzar=true (SUPER_ADMIN tipico), borra el cierre existente y lo
     *   recalcula. Util tras un backfill de facturas que cambia los totales.
     */
    @PostMapping("/cierre-dia/generar")
    public ResponseEntity<ApiResponse<com.usco.parqueaderos_api.report.dto.CierreDiaDTO>> generarCierre(
            @RequestParam Long parqueaderoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "false") boolean forzar) {
        return ResponseEntity.ok(ApiResponse.ok(
                cierreDiaService.generarCierre(parqueaderoId, fecha, forzar)));
    }
}
