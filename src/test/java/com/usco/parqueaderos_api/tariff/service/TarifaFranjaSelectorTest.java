package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.entity.TarifaFranja;
import com.usco.parqueaderos_api.tariff.repository.TarifaFranjaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class TarifaFranjaSelectorTest {

    private TarifaFranjaRepository repo;
    private TarifaFranjaSelector selector;
    private Tarifa tarifa;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(TarifaFranjaRepository.class);
        selector = new TarifaFranjaSelector(repo);
        tarifa = new Tarifa();
        tarifa.setId(1L);
    }

    private TarifaFranja franja(LocalTime ini, LocalTime fin, boolean finDeSemana, double valor) {
        TarifaFranja f = new TarifaFranja();
        f.setHoraInicio(ini);
        f.setHoraFin(fin);
        f.setSoloFinesDeSemana(finDeSemana);
        f.setValor(valor);
        f.setActiva(true);
        return f;
    }

    @Test
    @DisplayName("Franja diurna 06:00-18:00, entrada 10:00 -> aplica")
    void diurna_aplica() {
        TarifaFranja f = franja(LocalTime.of(6,0), LocalTime.of(18,0), false, 3000);
        when(repo.findByTarifaIdAndActivaTrue(anyLong())).thenReturn(List.of(f));
        Optional<TarifaFranja> r = selector.seleccionar(tarifa,
                LocalDateTime.of(2026, 5, 1, 10, 0));
        assertTrue(r.isPresent());
    }

    @Test
    @DisplayName("Franja diurna 06:00-18:00, entrada 22:00 -> NO aplica")
    void diurna_no_aplica_de_noche() {
        TarifaFranja f = franja(LocalTime.of(6,0), LocalTime.of(18,0), false, 3000);
        when(repo.findByTarifaIdAndActivaTrue(anyLong())).thenReturn(List.of(f));
        Optional<TarifaFranja> r = selector.seleccionar(tarifa,
                LocalDateTime.of(2026, 5, 1, 22, 0));
        assertFalse(r.isPresent());
    }

    @Test
    @DisplayName("Franja nocturna que cruza medianoche 22:00-06:00, entrada 02:00 -> aplica")
    void nocturna_cruza_medianoche() {
        TarifaFranja f = franja(LocalTime.of(22,0), LocalTime.of(6,0), false, 1500);
        when(repo.findByTarifaIdAndActivaTrue(anyLong())).thenReturn(List.of(f));
        Optional<TarifaFranja> r = selector.seleccionar(tarifa,
                LocalDateTime.of(2026, 5, 1, 2, 0));
        assertTrue(r.isPresent());
    }

    @Test
    @DisplayName("Franja solo fin de semana, entrada un viernes -> NO aplica")
    void fds_no_aplica_dia_semana() {
        TarifaFranja f = franja(LocalTime.of(0,0), LocalTime.of(23,59), true, 8000);
        when(repo.findByTarifaIdAndActivaTrue(anyLong())).thenReturn(List.of(f));
        // 2026-05-01 es viernes
        Optional<TarifaFranja> r = selector.seleccionar(tarifa,
                LocalDateTime.of(2026, 5, 1, 12, 0));
        assertFalse(r.isPresent());
    }

    @Test
    @DisplayName("Franja solo fin de semana, entrada un sabado -> aplica")
    void fds_aplica_sabado() {
        TarifaFranja f = franja(LocalTime.of(0,0), LocalTime.of(23,59), true, 8000);
        when(repo.findByTarifaIdAndActivaTrue(anyLong())).thenReturn(List.of(f));
        // 2026-05-02 es sabado
        Optional<TarifaFranja> r = selector.seleccionar(tarifa,
                LocalDateTime.of(2026, 5, 2, 12, 0));
        assertTrue(r.isPresent());
    }
}
