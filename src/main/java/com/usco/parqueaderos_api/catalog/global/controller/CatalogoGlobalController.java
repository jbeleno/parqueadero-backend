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
    private final EstadoCivilRepository estadoCivilRepo;
    private final PaisCodigoPlacaRepository paisCodigoPlacaRepo;
    private final TipoServicioVehiculoRepository tipoServicioVehiculoRepo;
    private final TipoAccesoDispositivoRepository tipoAccesoDispositivoRepo;
    private final CanalOrigenReservaRepository canalOrigenReservaRepo;

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

    @GetMapping("/estados-civiles")
    public ResponseEntity<ApiResponse<List<EstadoCivil>>> estadosCiviles() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoCivilRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Estados civiles"));
    }

    @GetMapping("/paises-placa")
    public ResponseEntity<ApiResponse<List<PaisCodigoPlaca>>> paisesPlaca() {
        return ResponseEntity.ok(ApiResponse.ok(
                paisCodigoPlacaRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Paises de placa"));
    }

    @GetMapping("/tipos-servicio-vehiculo")
    public ResponseEntity<ApiResponse<List<TipoServicioVehiculo>>> tiposServicioVehiculo() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoServicioVehiculoRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Tipos de servicio de vehiculo"));
    }

    @GetMapping("/tipos-acceso-dispositivo")
    public ResponseEntity<ApiResponse<List<TipoAccesoDispositivo>>> tiposAccesoDispositivo() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoAccesoDispositivoRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Tipos de acceso de dispositivo"));
    }

    @GetMapping("/canales-origen-reserva")
    public ResponseEntity<ApiResponse<List<CanalOrigenReserva>>> canalesOrigenReserva() {
        return ResponseEntity.ok(ApiResponse.ok(
                canalOrigenReservaRepo.findByActivoTrueOrderByOrdenDisplayAsc(),
                "Canales de origen de reserva"));
    }
}
