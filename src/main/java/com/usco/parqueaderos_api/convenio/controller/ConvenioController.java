package com.usco.parqueaderos_api.convenio.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.convenio.dto.ConvenioDTO;
import com.usco.parqueaderos_api.convenio.dto.ValidacionCompraDTO;
import com.usco.parqueaderos_api.convenio.service.ConvenioService;
import com.usco.parqueaderos_api.convenio.service.ValidacionCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/convenios")
@RequiredArgsConstructor
public class ConvenioController {

    private final ConvenioService convenioService;
    private final ValidacionCompraService validacionService;

    @GetMapping("/por-parqueadero/{parqueaderoId}")
    public ResponseEntity<ApiResponse<List<ConvenioDTO>>> porParqueadero(@PathVariable Long parqueaderoId) {
        return ResponseEntity.ok(ApiResponse.ok(convenioService.findByParqueadero(parqueaderoId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<ConvenioDTO>> crear(@RequestBody ConvenioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(convenioService.save(dto)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<ConvenioDTO>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(convenioService.desactivar(id)));
    }

    /**
     * Registra el comprobante de compra de un comercio sobre un ticket EN_CURSO.
     * El operario de caja la registra cuando el cliente presenta la factura del comercio.
     */
    @PostMapping("/validacion-compra")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO','OPERARIO_CAJA')")
    public ResponseEntity<ApiResponse<ValidacionCompraDTO>> validarCompra(@RequestBody ValidacionCompraDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(validacionService.registrar(dto)));
    }
}
