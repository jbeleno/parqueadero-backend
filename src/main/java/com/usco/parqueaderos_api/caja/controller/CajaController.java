package com.usco.parqueaderos_api.caja.controller;

import com.usco.parqueaderos_api.caja.dto.CajaDTO;
import com.usco.parqueaderos_api.caja.dto.MovimientoCajaDTO;
import com.usco.parqueaderos_api.caja.entity.Caja;
import com.usco.parqueaderos_api.caja.entity.MovimientoCaja;
import com.usco.parqueaderos_api.caja.service.CajaService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERARIO_CAJA','ADMIN_PARQUEADERO','ADMIN','SUPER_ADMIN')")
public class CajaController {

    private final CajaService service;

    @PostMapping("/abrir")
    public ResponseEntity<ApiResponse<CajaDTO>> abrir(@RequestBody AbrirRequest req) {
        Caja c = service.abrir(req.getParqueaderoId(), req.getFondoInicial(),
                req.getNombre(), req.getObservaciones());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toDTO(c)));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<ApiResponse<CajaDTO>> cerrar(@PathVariable Long id,
                                                       @RequestBody CerrarRequest req) {
        Caja c = service.cerrar(id, req.getSaldoContado(), req.getObservaciones());
        return ResponseEntity.ok(ApiResponse.ok(toDTO(c)));
    }

    @PostMapping("/{id}/retirar")
    @PreAuthorize("hasAnyRole('ADMIN_PARQUEADERO','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoCajaDTO>> retirar(@PathVariable Long id,
                                                                   @RequestBody MontoMotivoRequest req) {
        MovimientoCaja m = service.retirar(id, req.getMonto(), req.getMotivo());
        return ResponseEntity.ok(ApiResponse.ok(toMovDTO(m)));
    }

    @PostMapping("/{id}/depositar")
    @PreAuthorize("hasAnyRole('ADMIN_PARQUEADERO','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoCajaDTO>> depositar(@PathVariable Long id,
                                                                     @RequestBody MontoMotivoRequest req) {
        MovimientoCaja m = service.depositar(id, req.getMonto(), req.getMotivo());
        return ResponseEntity.ok(ApiResponse.ok(toMovDTO(m)));
    }

    @PostMapping("/{id}/ajustar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoCajaDTO>> ajustar(@PathVariable Long id,
                                                                   @RequestBody MontoMotivoRequest req) {
        MovimientoCaja m = service.ajustar(id, req.getMonto(), req.getMotivo());
        return ResponseEntity.ok(ApiResponse.ok(toMovDTO(m)));
    }

    @GetMapping("/mi-caja-abierta")
    public ResponseEntity<ApiResponse<CajaDTO>> miCajaAbierta() {
        Optional<Caja> c = service.miCajaAbierta();
        return ResponseEntity.ok(ApiResponse.ok(c.map(this::toDTO).orElse(null)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CajaDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toDTO(service.findById(id))));
    }

    @GetMapping("/{id}/movimientos")
    public ResponseEntity<ApiResponse<List<MovimientoCajaDTO>>> movimientos(@PathVariable Long id) {
        List<MovimientoCajaDTO> list = service.movimientos(id).stream().map(this::toMovDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/parqueadero/{parqueaderoId}")
    public ResponseEntity<ApiResponse<List<CajaDTO>>> listarPorParqueadero(@PathVariable Long parqueaderoId) {
        List<CajaDTO> list = service.listarPorParqueadero(parqueaderoId).stream().map(this::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // ── Mapeo a DTO ──

    private CajaDTO toDTO(Caja c) {
        return new CajaDTO(
                c.getId(),
                c.getParqueadero() != null ? c.getParqueadero().getId() : null,
                c.getParqueadero() != null ? c.getParqueadero().getNombre() : null,
                c.getUsuario() != null ? c.getUsuario().getId() : null,
                c.getUsuario() != null ? c.getUsuario().getCorreo() : null,
                c.getNombre(),
                c.getFondoInicial(),
                c.getSaldoCalculado(),
                c.getSaldoContado(),
                c.getDiferencia(),
                c.getEstado(),
                c.getAbiertaEn(),
                c.getCerradaEn(),
                c.getObservacionesApertura(),
                c.getObservacionesCierre()
        );
    }

    private MovimientoCajaDTO toMovDTO(MovimientoCaja m) {
        return new MovimientoCajaDTO(
                m.getId(),
                m.getCaja() != null ? m.getCaja().getId() : null,
                m.getTipo() != null ? m.getTipo().name() : null,
                m.getMonto(),
                m.getPago() != null ? m.getPago().getId() : null,
                m.getMotivo(),
                m.getUsuario() != null ? m.getUsuario().getId() : null,
                m.getSaldoResultante(),
                m.getFechaHora()
        );
    }

    // ── Requests ──

    @Data public static class AbrirRequest {
        private Long parqueaderoId;
        private Double fondoInicial;
        private String nombre;
        private String observaciones;
    }
    @Data public static class CerrarRequest {
        private Double saldoContado;
        private String observaciones;
    }
    @Data public static class MontoMotivoRequest {
        private Double monto;
        private String motivo;
    }
}
