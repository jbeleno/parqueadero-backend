package com.usco.parqueaderos_api.ticket.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.ticket.dto.TicketDTO;
import com.usco.parqueaderos_api.ticket.dto.TicketManualDTO;
import com.usco.parqueaderos_api.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketDTO>> create(@Valid @RequestBody TicketDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(ticketService.save(dto)));
    }

    /**
     * Registra entrada SIN OCR: el operario digita placa o la deja vacia (visitante).
     * Atomico: crea Vehiculo (si no existe) y Ticket en una sola transaccion.
     * Caso de uso: la camara no leyo la placa (vidrio sucio, sin luz, falla HW).
     */
    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<TicketDTO>> createManual(@Valid @RequestBody TicketManualDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ticketService.createManual(dto), "Ticket registrado manualmente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDTO>> update(@PathVariable Long id, @Valid @RequestBody TicketDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.update(id, dto)));
    }

    /**
     * Registra la salida de un ticket EN_CURSO.
     * El backend setea fechaHoraSalida con el reloj del servidor y calcula
     * el monto automaticamente via TarifaCalculatorService.
     * No requiere body.
     */
    @PatchMapping("/{id}/salida")
    public ResponseEntity<ApiResponse<TicketDTO>> registrarSalida(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(ticketService.registrarSalida(id), "Salida registrada y monto calculado"));
    }

    /**
     * Mueve un ticket EN_CURSO a otro punto de parqueo del mismo parqueadero.
     * Util cuando el OCR asigno auto un punto y el operador necesita corregir
     * a donde realmente se estaciono el vehiculo. Body: { "puntoParqueoId": X }.
     */
    @PatchMapping("/{id}/punto")
    public ResponseEntity<ApiResponse<TicketDTO>> cambiarPunto(
            @PathVariable Long id, @RequestBody CambiarPuntoRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.cambiarPunto(id, body.puntoParqueoId()),
                "Ticket movido al nuevo punto"));
    }

    public record CambiarPuntoRequest(Long puntoParqueoId) {}

    /** Anula un ticket EN_CURSO o CERRADO. Body: { "motivo": "..." } */
    @PatchMapping("/{id}/anular")
    public ResponseEntity<ApiResponse<TicketDTO>> anular(
            @PathVariable Long id, @RequestBody AnularTicketRequest body) {
        return ResponseEntity.ok(
                ApiResponse.ok(ticketService.anular(id, body.motivo()), "Ticket anulado"));
    }

    public record AnularTicketRequest(String motivo) {}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
