package com.usco.parqueaderos_api.subscription.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.subscription.dto.AbonarSaldoRequest;
import com.usco.parqueaderos_api.subscription.dto.MovimientoSaldoDTO;
import com.usco.parqueaderos_api.subscription.dto.SuscripcionDTO;
import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.MovimientoSaldo;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import com.usco.parqueaderos_api.subscription.service.SaldoService;
import com.usco.parqueaderos_api.subscription.service.SuscripcionService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/saldos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class SaldoController {

    private final SaldoService saldoService;
    private final SuscripcionService suscripcionService;
    private final SuscripcionRepository suscripcionRepo;

    /**
     * Carga saldo a un vehiculo. Si ya existe una suscripcion ABONO_PREPAGO ACTIVA,
     * suma al saldo. Si no, crea una nueva con saldo = monto.
     */
    @PostMapping("/abonar")
    @Transactional
    public ResponseEntity<ApiResponse<SuscripcionDTO>> abonar(
            @Valid @RequestBody AbonarSaldoRequest req) {

        Optional<Suscripcion> existente = suscripcionRepo
                .findFirstByVehiculoIdAndParqueaderoIdAndTipoAndEstado(
                        req.getVehiculoId(), req.getParqueaderoId(),
                        TipoSuscripcion.ABONO_PREPAGO, EstadoSuscripcion.ACTIVA);

        Suscripcion s;
        if (existente.isPresent()) {
            s = existente.get();
            saldoService.abonar(s, req.getMonto(), "Recarga de saldo");
        } else {
            s = suscripcionService.crear(req.getVehiculoId(), req.getParqueaderoId(),
                    req.getTarifaId(), TipoSuscripcion.ABONO_PREPAGO, req.getMonto());
        }
        return ResponseEntity.ok(ApiResponse.ok(
                SuscripcionDTO.builder()
                        .id(s.getId())
                        .vehiculoId(s.getVehiculo().getId())
                        .vehiculoPlaca(s.getVehiculo().getPlaca())
                        .parqueaderoId(s.getParqueadero().getId())
                        .parqueaderoNombre(s.getParqueadero().getNombre())
                        .tipo(s.getTipo())
                        .estado(s.getEstado())
                        .fechaInicio(s.getFechaInicio())
                        .fechaFin(s.getFechaFin())
                        .montoPagado(s.getMontoPagado())
                        .saldoRestante(s.getSaldoRestante())
                        .build(),
                "Saldo abonado"));
    }

    @GetMapping("/suscripcion/{id}/movimientos")
    public ResponseEntity<ApiResponse<List<MovimientoSaldoDTO>>> historial(@PathVariable Long id) {
        List<MovimientoSaldoDTO> list = saldoService.historial(id).stream()
                .map(this::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    private MovimientoSaldoDTO toDTO(MovimientoSaldo m) {
        return MovimientoSaldoDTO.builder()
                .id(m.getId())
                .suscripcionId(m.getSuscripcion() != null ? m.getSuscripcion().getId() : null)
                .monto(m.getMonto())
                .ticketId(m.getTicket() != null ? m.getTicket().getId() : null)
                .pagoId(m.getPago() != null ? m.getPago().getId() : null)
                .saldoResultante(m.getSaldoResultante())
                .tipo(m.getTipo())
                .motivo(m.getMotivo())
                .fecha(m.getFecha())
                .build();
    }

    /**
     * Ajuste manual de saldo por SUPER_ADMIN con motivo obligatorio (auditable).
     * monto puede ser positivo (regalo/correccion +) o negativo (penalizacion/correccion -).
     */
    @PostMapping("/suscripcion/{id}/ajustar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoSaldoDTO>> ajustar(
            @PathVariable Long id,
            @RequestBody AjusteSaldoRequest req) {
        MovimientoSaldo m = saldoService.ajustar(id, req.getMonto(), req.getMotivo());
        return ResponseEntity.ok(ApiResponse.ok(toDTO(m), "Ajuste registrado"));
    }

    @Data
    public static class AjusteSaldoRequest {
        private Double monto;
        private String motivo;
    }
}
