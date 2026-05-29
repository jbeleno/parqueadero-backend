package com.usco.parqueaderos_api.catalog.global.controller;

import com.usco.parqueaderos_api.catalog.global.entity.*;
import com.usco.parqueaderos_api.catalog.global.repository.*;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de solo lectura para los 6 catalogos globales v49 Fase 1.
 *
 * Frontend los consume para popular dropdowns/selects. Cualquier
 * usuario autenticado los puede leer (no sensibles, son de UI).
 *
 * CRUD para SUPER_ADMIN viene en sesion futura.
 */
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoGlobalController {

    private final TipoDocumentoRepository tipoDocumentoRepo;
    private final GeneroRepository generoRepo;
    private final MonedaRepository monedaRepo;
    private final ZonaHorariaRepository zonaHorariaRepo;
    private final UnidadTarifaRepository unidadTarifaRepo;
    private final RegimenTributarioRepository regimenTributarioRepo;

    @GetMapping("/tipos-documento")
    public ResponseEntity<ApiResponse<List<TipoDocumento>>> tiposDocumento() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoDocumentoRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Tipos de documento"));
    }

    @GetMapping("/generos")
    public ResponseEntity<ApiResponse<List<Genero>>> generos() {
        return ResponseEntity.ok(ApiResponse.ok(
                generoRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Generos"));
    }

    @GetMapping("/monedas")
    public ResponseEntity<ApiResponse<List<Moneda>>> monedas() {
        return ResponseEntity.ok(ApiResponse.ok(
                monedaRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Monedas"));
    }

    @GetMapping("/zonas-horarias")
    public ResponseEntity<ApiResponse<List<ZonaHoraria>>> zonasHorarias() {
        return ResponseEntity.ok(ApiResponse.ok(
                zonaHorariaRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Zonas horarias"));
    }

    @GetMapping("/unidades-tarifa")
    public ResponseEntity<ApiResponse<List<UnidadTarifa>>> unidadesTarifa() {
        return ResponseEntity.ok(ApiResponse.ok(
                unidadTarifaRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Unidades de tarifa"));
    }

    @GetMapping("/regimenes-tributarios")
    public ResponseEntity<ApiResponse<List<RegimenTributario>>> regimenesTributarios() {
        return ResponseEntity.ok(ApiResponse.ok(
                regimenTributarioRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Regimenes tributarios"));
    }
}
