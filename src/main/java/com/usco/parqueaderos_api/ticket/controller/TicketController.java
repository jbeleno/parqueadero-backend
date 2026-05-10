package com.usco.parqueaderos_api.ticket.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.ticket.dto.TicketDTO;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
