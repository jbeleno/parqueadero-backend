package com.usco.parqueaderos_api.subscription.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.subscription.dto.CrearSuscripcionRequest;
import com.usco.parqueaderos_api.subscription.dto.SuscripcionDTO;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SuscripcionDTO>> crear(
            @Valid @RequestBody CrearSuscripcionRequest req) {
        Suscripcion s = suscripcionService.crear(
                req.getVehiculoId(), req.getParqueaderoId(), req.getTarifaId(),
                req.getTipo(), req.getMontoPagado(),
                req.getPuntoParqueoReservadoId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toDTO(s), "Suscripcion creada"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SuscripcionDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toDTO(suscripcionService.findById(id))));
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public ResponseEntity<ApiResponse<List<SuscripcionDTO>>> byVehiculo(
            @PathVariable Long vehiculoId) {
        List<SuscripcionDTO> list = suscripcionService.findByVehiculo(vehiculoId)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/parqueadero/{parqueaderoId}/activas")
    public ResponseEntity<ApiResponse<List<SuscripcionDTO>>> activasPorParq(
            @PathVariable Long parqueaderoId) {
        List<SuscripcionDTO> list = suscripcionService.findActivasPorParqueadero(parqueaderoId)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<SuscripcionDTO>> cancelar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean reembolsar) {
        Suscripcion s = suscripcionService.cancelar(id, reembolsar);
        return ResponseEntity.ok(ApiResponse.ok(toDTO(s), "Suscripcion cancelada"));
    }

    private SuscripcionDTO toDTO(Suscripcion s) {
        return SuscripcionDTO.builder()
                .id(s.getId())
                .vehiculoId(s.getVehiculo() != null ? s.getVehiculo().getId() : null)
                .vehiculoPlaca(s.getVehiculo() != null ? s.getVehiculo().getPlaca() : null)
                .parqueaderoId(s.getParqueadero() != null ? s.getParqueadero().getId() : null)
                .parqueaderoNombre(s.getParqueadero() != null ? s.getParqueadero().getNombre() : null)
                .tarifaId(s.getTarifa() != null ? s.getTarifa().getId() : null)
                .tipo(s.getTipo())
                .estado(s.getEstado())
                .fechaInicio(s.getFechaInicio())
                .fechaFin(s.getFechaFin())
                .montoPagado(s.getMontoPagado())
                .saldoRestante(s.getSaldoRestante())
                .fechaCreacion(s.getFechaCreacion())
                .puntoParqueoReservadoId(s.getPuntoParqueoReservado() != null
                        ? s.getPuntoParqueoReservado().getId() : null)
                .puntoParqueoReservadoNombre(s.getPuntoParqueoReservado() != null
                        ? s.getPuntoParqueoReservado().getNombre() : null)
                .build();
    }
}
