package com.usco.parqueaderos_api.subscription.service;

import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
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

class SuscripcionAvisoVencimientoJobTest {

    private SuscripcionRepository repo;
    private NotificationService notif;
    private SuscripcionAvisoVencimientoJob job;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(SuscripcionRepository.class);
        notif = Mockito.mock(NotificationService.class);
        job = new SuscripcionAvisoVencimientoJob(repo, notif);
        ReflectionTestUtils.setField(job, "avisoDias", 3);
    }

    private Suscripcion susc(Long id, Long parqueaderoId, String placa, LocalDateTime fin) {
        Suscripcion s = new Suscripcion();
        s.setId(id);
        s.setFechaFin(fin);
        s.setTipo(TipoSuscripcion.MENSUAL);
        Parqueadero p = new Parqueadero(); p.setId(parqueaderoId); s.setParqueadero(p);
        Vehiculo v = new Vehiculo(); v.setPlaca(placa); s.setVehiculo(v);
        return s;
    }

    @Test
    @DisplayName("Sin proximas a vencer -> no notifica")
    void sin_proximas() {
        when(repo.findProximasAVencer(any(), any(), eq(null))).thenReturn(List.of());
        job.notificarProximasAVencer();
        verifyNoInteractions(notif);
    }

    @Test
    @DisplayName("Proximas a vencer -> notifica tipo SUSCRIPCION_PROXIMA_A_VENCER")
    void notifica() {
        when(repo.findProximasAVencer(any(), any(), eq(null))).thenReturn(List.of(
                susc(1L, 5L, "AAA111", LocalDateTime.now().plusDays(2))
        ));
        job.notificarProximasAVencer();
        ArgumentCaptor<NotificacionDTO> cap = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notif).notificarParqueadero(eq(5L), cap.capture());
        assertEquals("SUSCRIPCION_PROXIMA_A_VENCER", cap.getValue().getTipo());
    }
}
