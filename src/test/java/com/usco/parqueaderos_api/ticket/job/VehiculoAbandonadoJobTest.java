package com.usco.parqueaderos_api.ticket.job;

import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VehiculoAbandonadoJobTest {

    private TicketRepository ticketRepo;
    private NotificationService notif;
    private VehiculoAbandonadoJob job;

    @BeforeEach
    void setup() {
        ticketRepo = Mockito.mock(TicketRepository.class);
        notif = Mockito.mock(NotificationService.class);
        job = new VehiculoAbandonadoJob(ticketRepo, notif);
        ReflectionTestUtils.setField(job, "umbralDias", 30);
    }

    private Ticket ticket(Long id, String placa, Long parqueaderoId, LocalDateTime entrada) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setFechaHoraEntrada(entrada);
        Vehiculo v = new Vehiculo(); v.setPlaca(placa); t.setVehiculo(v);
        Parqueadero p = new Parqueadero(); p.setId(parqueaderoId); t.setParqueadero(p);
        return t;
    }

    @Test
    @DisplayName("Sin candidatos -> no notifica")
    void sin_candidatos() {
        when(ticketRepo.findEnCursoEntradaAntesDe(any())).thenReturn(List.of());
        job.detectarAbandonados();
        verifyNoInteractions(notif);
    }

    @Test
    @DisplayName("Candidatos -> notifica por parqueadero con tipo correcto")
    void notifica() {
        when(ticketRepo.findEnCursoEntradaAntesDe(any())).thenReturn(List.of(
                ticket(1L, "AAA111", 7L, LocalDateTime.now().minusDays(45)),
                ticket(2L, "BBB222", 7L, LocalDateTime.now().minusDays(60))
        ));
        job.detectarAbandonados();
        ArgumentCaptor<NotificacionDTO> cap = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notif, times(2)).notificarParqueadero(eq(7L), cap.capture());
        cap.getAllValues().forEach(n ->
                assertEquals("VEHICULO_ABANDONADO", n.getTipo()));
    }
}
