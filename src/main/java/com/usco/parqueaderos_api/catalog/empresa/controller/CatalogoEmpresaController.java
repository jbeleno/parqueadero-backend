package com.usco.parqueaderos_api.catalog.empresa.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.empresa.entity.*;
import com.usco.parqueaderos_api.catalog.empresa.repository.*;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lectura de catalogos POR EMPRESA. v49 Fase 2.
 * El alcance lo determina el JWT del usuario (su empresa_id).
 */
@RestController
@RequestMapping("/api/catalogos/empresa")
@RequiredArgsConstructor
public class CatalogoEmpresaController {

    private final EmpresaMetodoPagoRepository metodoPagoRepo;
    private final EstadoTicketRepository estadoTicketRepo;
    private final EstadoFacturaRepository estadoFacturaRepo;
    private final EstadoPagoRepository estadoPagoRepo;
    private final EstadoSuscripcionRepository estadoSuscripcionRepo;
    private final EstadoCajaRepository estadoCajaRepo;
    private final TipoMovimientoCajaRepository tipoMovimientoCajaRepo;
    private final TipoMovimientoSaldoRepository tipoMovimientoSaldoRepo;
    private final TipoDescuentoConvenioRepository tipoDescuentoConvenioRepo;
    private final OrigenFacturaRepository origenFacturaRepo;
    private final TipoResolucionDianRepository tipoResolucionDianRepo;
    private final CurrentUserService currentUser;

    private Long empresaIdOrFail() {
        return currentUser.getCurrentEmpresaId().orElseThrow(
                () -> new AccessDeniedException("Usuario sin empresa asociada"));
    }

    @GetMapping("/metodos-pago")
    public ResponseEntity<ApiResponse<List<EmpresaMetodoPago>>> metodosPago() {
        return ResponseEntity.ok(ApiResponse.ok(
                metodoPagoRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Metodos de pago"));
    }

    @GetMapping("/estados-ticket")
    public ResponseEntity<ApiResponse<List<EstadoTicket>>> estadosTicket() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoTicketRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Estados de ticket"));
    }

    @GetMapping("/estados-factura")
    public ResponseEntity<ApiResponse<List<EstadoFactura>>> estadosFactura() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoFacturaRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Estados de factura"));
    }

    @GetMapping("/estados-pago")
    public ResponseEntity<ApiResponse<List<EstadoPago>>> estadosPago() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoPagoRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Estados de pago"));
    }

    @GetMapping("/estados-suscripcion")
    public ResponseEntity<ApiResponse<List<EstadoSuscripcion>>> estadosSuscripcion() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoSuscripcionRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Estados de suscripcion"));
    }

    @GetMapping("/estados-caja")
    public ResponseEntity<ApiResponse<List<EstadoCaja>>> estadosCaja() {
        return ResponseEntity.ok(ApiResponse.ok(
                estadoCajaRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Estados de caja"));
    }

    @GetMapping("/tipos-movimiento-caja")
    public ResponseEntity<ApiResponse<List<TipoMovimientoCaja>>> tiposMovCaja() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoMovimientoCajaRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Tipos de movimiento de caja"));
    }

    @GetMapping("/tipos-movimiento-saldo")
    public ResponseEntity<ApiResponse<List<TipoMovimientoSaldo>>> tiposMovSaldo() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoMovimientoSaldoRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Tipos de movimiento de saldo"));
    }

    @GetMapping("/tipos-descuento-convenio")
    public ResponseEntity<ApiResponse<List<TipoDescuentoConvenio>>> tiposDescConvenio() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoDescuentoConvenioRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Tipos de descuento de convenio"));
    }

    @GetMapping("/origenes-factura")
    public ResponseEntity<ApiResponse<List<OrigenFactura>>> origenesFactura() {
        return ResponseEntity.ok(ApiResponse.ok(
                origenFacturaRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Origenes de factura"));
    }

    @GetMapping("/tipos-resolucion-dian")
    public ResponseEntity<ApiResponse<List<TipoResolucionDian>>> tiposResolucionDian() {
        return ResponseEntity.ok(ApiResponse.ok(
                tipoResolucionDianRepo.findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(empresaIdOrFail()),
                "Tipos de resolucion DIAN"));
    }
}
