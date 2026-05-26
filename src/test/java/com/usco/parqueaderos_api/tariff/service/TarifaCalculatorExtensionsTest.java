package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.entity.TarifaFranja;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests para extensiones del calculador:
 * - Unidad POR_MINUTO
 * - Tope legal por minuto en parqueadero
 * - Desagregado de IVA
 * - Franja horaria que sustituye al valor base
 */
class TarifaCalculatorExtensionsTest {

    private Ticket ticket(Tarifa t, LocalDateTime entrada) {
        Ticket tk = new Ticket();
        tk.setTarifa(t);
        tk.setFechaHoraEntrada(entrada);
        return tk;
    }

    private Tarifa basica(double valor, String unidad) {
        Tarifa t = new Tarifa();
        t.setValor(valor);
        t.setUnidad(unidad);
        t.setMinutosGracia(0);
        t.setValorMinimo(0.0);
        t.setMinutosCubiertosPorMinimo(0);
        return t;
    }

    @Test
    @DisplayName("POR_MINUTO cobra exactamente valor x minutos sin redondeo")
    void porMinuto_cobra_lineal() {
        TarifaCalculatorService calc = new TarifaCalculatorService();
        LocalDateTime e = LocalDateTime.of(2026, 5, 1, 10, 0);
        Tarifa t = basica(50, "POR_MINUTO");
        Ticket tk = ticket(t, e);
        assertEquals(50 * 17, calc.calcular(tk, e.plusMinutes(17)), 0.001);
        assertEquals(50 * 1, calc.calcular(tk, e.plusMinutes(1)), 0.001);
    }

    @Test
    @DisplayName("Tope legal por minuto trunca el total si la tarifa lo excede")
    void tope_legal_trunca() {
        TarifaCalculatorService calc = new TarifaCalculatorService();
        LocalDateTime e = LocalDateTime.of(2026, 5, 1, 10, 0);
        // tarifa abusiva: 200/min, parqueadero topa en 100/min
        Tarifa t = basica(200, "POR_MINUTO");
        Parqueadero p = new Parqueadero();
        p.setTarifaMaximaPorMinuto(100.0);
        t.setParqueadero(p);
        Ticket tk = ticket(t, e);
        // 10 min: tope = 100 * 10 = 1000
        assertEquals(1000.0, calc.calcular(tk, e.plusMinutes(10)), 0.001);
    }

    @Test
    @DisplayName("Desagregado de IVA 19% sobre total")
    void iva_desagregado() {
        TarifaCalculatorService calc = new TarifaCalculatorService();
        Tarifa t = new Tarifa();
        t.setAplicaIva(true);
        t.setIvaPorcentaje(19.0);
        TarifaCalculatorService.BreakdownIva b = calc.desagregarIva(t, 11900.0);
        assertEquals(10000.0, b.base(), 0.5);
        assertEquals(1900.0, b.iva(), 0.5);
        assertEquals(11900.0, b.total(), 0.001);
    }

    @Test
    @DisplayName("Sin IVA: base = total y IVA = 0")
    void iva_no_aplica() {
        TarifaCalculatorService calc = new TarifaCalculatorService();
        Tarifa t = new Tarifa();
        t.setAplicaIva(false);
        TarifaCalculatorService.BreakdownIva b = calc.desagregarIva(t, 5000.0);
        assertEquals(5000.0, b.base(), 0.001);
        assertEquals(0.0, b.iva(), 0.001);
    }

    @Test
    @DisplayName("Franja horaria sustituye al valor base cuando aplica")
    void franja_aplica_y_sustituye() {
        TarifaFranjaSelector selector = mock(TarifaFranjaSelector.class);
        TarifaCalculatorService calc = new TarifaCalculatorService(selector);
        LocalDateTime entradaNocturna = LocalDateTime.of(2026, 5, 1, 23, 0);
        Tarifa t = basica(5000, "POR_HORA");
        TarifaFranja f = new TarifaFranja();
        f.setValor(2000.0); // tarifa nocturna mas barata
        when(selector.seleccionar(any(), any())).thenReturn(Optional.of(f));
        Ticket tk = ticket(t, entradaNocturna);
        // 60 min con valor 2000/h = 2000 (en vez de 5000)
        assertEquals(2000.0, calc.calcular(tk, entradaNocturna.plusMinutes(60)), 0.001);
    }

    @Test
    @DisplayName("Sin franja: usa valor base")
    void sin_franja_usa_base() {
        TarifaFranjaSelector selector = mock(TarifaFranjaSelector.class);
        TarifaCalculatorService calc = new TarifaCalculatorService(selector);
        LocalDateTime e = LocalDateTime.of(2026, 5, 1, 10, 0);
        Tarifa t = basica(5000, "POR_HORA");
        when(selector.seleccionar(any(), any())).thenReturn(Optional.empty());
        Ticket tk = ticket(t, e);
        assertEquals(5000.0, calc.calcular(tk, e.plusMinutes(60)), 0.001);
    }
}
