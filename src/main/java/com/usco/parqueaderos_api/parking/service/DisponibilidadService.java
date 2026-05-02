package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.DisponibilidadDTO;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final PuntoParqueoRepository puntoParqueoRepository;
    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final ParqueaderoRepository parqueaderoRepository;

    @Transactional(readOnly = true)
    public DisponibilidadDTO calcular(Long parqueaderoId) {
        if (!parqueaderoRepository.existsById(parqueaderoId)) {
            throw new ResourceNotFoundException("Parqueadero", parqueaderoId);
        }
        long total = puntoParqueoRepository.countByParqueaderoId(parqueaderoId);
        long ocupados = ticketRepository.countOcupadosEnParqueadero(parqueaderoId);
        long reservados = reservaRepository.countReservadosEnParqueadero(parqueaderoId, LocalDateTime.now());
        long disponibles = Math.max(0L, total - ocupados - reservados);
        return new DisponibilidadDTO(parqueaderoId, total, disponibles, ocupados, reservados);
    }
}
