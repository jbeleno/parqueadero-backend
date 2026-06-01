package com.usco.parqueaderos_api.catalog.global.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.global.entity.*;
import com.usco.parqueaderos_api.catalog.global.service.CatalogoResolverService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de lectura para los 11 catalogos globales.
 *
 * v50: cada GET aplica el {@link CatalogoResolverService} que filtra los items
 * canonicos segun la configuracion de la empresa del usuario y agrega los
 * items custom propios de esa empresa.
 *
 * Si el usuario no tiene empresa asociada (SUPER_ADMIN), devuelve solo los
 * globales activos.
 *
 * Si la empresa no configuro restricciones (no tiene filas en
 * empresa_catalogo_global_activo), acepta TODOS los globales por default.
 */
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogoGlobalController {

    private final CatalogoResolverService resolver;
    private final CurrentUserService currentUser;

    private Long empresaIdOrNull() {
        return currentUser.getCurrentEmpresaId().orElse(null);
    }

    @GetMapping("/tipos-documento")
    public ResponseEntity<ApiResponse<List<TipoDocumento>>> tiposDocumento() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverTiposDocumento(empresaIdOrNull()),
                "Tipos de documento"));
    }

    @GetMapping("/generos")
    public ResponseEntity<ApiResponse<List<Genero>>> generos() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverGeneros(empresaIdOrNull()),
                "Generos"));
    }

    @GetMapping("/monedas")
    public ResponseEntity<ApiResponse<List<Moneda>>> monedas() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverMonedas(empresaIdOrNull()),
                "Monedas"));
    }

    @GetMapping("/zonas-horarias")
    public ResponseEntity<ApiResponse<List<ZonaHoraria>>> zonasHorarias() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverZonasHorarias(empresaIdOrNull()),
                "Zonas horarias"));
    }

    @GetMapping("/unidades-tarifa")
    public ResponseEntity<ApiResponse<List<UnidadTarifa>>> unidadesTarifa() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverUnidadesTarifa(empresaIdOrNull()),
                "Unidades de tarifa"));
    }

    @GetMapping("/regimenes-tributarios")
    public ResponseEntity<ApiResponse<List<RegimenTributario>>> regimenesTributarios() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverRegimenesTributarios(empresaIdOrNull()),
                "Regimenes tributarios"));
    }

    @GetMapping("/estados-civiles")
    public ResponseEntity<ApiResponse<List<EstadoCivil>>> estadosCiviles() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverEstadosCiviles(empresaIdOrNull()),
                "Estados civiles"));
    }

    @GetMapping("/paises-placa")
    public ResponseEntity<ApiResponse<List<PaisCodigoPlaca>>> paisesPlaca() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverPaisesPlaca(empresaIdOrNull()),
                "Paises de placa"));
    }

    @GetMapping("/tipos-servicio-vehiculo")
    public ResponseEntity<ApiResponse<List<TipoServicioVehiculo>>> tiposServicioVehiculo() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverTiposServicioVehiculo(empresaIdOrNull()),
                "Tipos de servicio de vehiculo"));
    }

    @GetMapping("/tipos-acceso-dispositivo")
    public ResponseEntity<ApiResponse<List<TipoAccesoDispositivo>>> tiposAccesoDispositivo() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverTiposAccesoDispositivo(empresaIdOrNull()),
                "Tipos de acceso de dispositivo"));
    }

    @GetMapping("/canales-origen-reserva")
    public ResponseEntity<ApiResponse<List<CanalOrigenReserva>>> canalesOrigenReserva() {
        return ResponseEntity.ok(ApiResponse.ok(
                resolver.resolverCanalesOrigenReserva(empresaIdOrNull()),
                "Canales de origen de reserva"));
    }
}
