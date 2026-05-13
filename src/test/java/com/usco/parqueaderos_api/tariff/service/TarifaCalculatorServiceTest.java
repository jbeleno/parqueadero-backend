package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de calculo de tarifa. Cubren los 4 modos: POR_HORA,
 * POR_FRACCION, POR_DIA, PLANA. Tambien edge cases (duracion 0,
 * tarifa null, salida antes de entrada).
 */
class TarifaCalculatorServiceTest {

    private TarifaCalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new TarifaCalculatorService();
    }

    private Ticket ticketCon(Tarifa t, LocalDateTime entrada) {
        Ticket ticket = new Ticket();
        ticket.setTarifa(t);
        ticket.setFechaHoraEntrada(entrada);
        return ticket;
    }

    private Tarifa tarifa(String unidad, double valor, Integer minutosFraccion) {
        Tarifa t = new Tarifa();
        t.setUnidad(unidad);
        t.setValor(valor);
        t.setMinutosFraccion(minutosFraccion);
        return t;
    }

    @Test
    @DisplayName("POR_HORA: 1h 30min cobra 2 horas (redondeo arriba)")
    void porHora_redondeaArriba() {
        Tarifa t = tarifa("POR_HORA", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        LocalDateTime salida = entrada.plusMinutes(90);
        double monto = calculator.calcular(ticketCon(t, entrada), salida);
        assertEquals(6000.0, monto);
    }

    @Test
    @DisplayName("POR_HORA: exactamente 60min cobra 1 hora")
    void porHora_exacta() {
        Tarifa t = tarifa("POR_HORA", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(1));
        assertEquals(3000.0, monto);
    }

    @Test
    @DisplayName("POR_FRACCION 15min: 20 min cobra 2 fracciones")
    void porFraccion_15min() {
        Tarifa t = tarifa("POR_FRACCION", 800, 15);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusMinutes(20));
        assertEquals(1600.0, monto);
    }

    @Test
    @DisplayName("POR_DIA: 25 horas cobra 2 dias")
    void porDia_redondeoSubeAUnDiaMas() {
        Tarifa t = tarifa("POR_DIA", 25000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(25));
        assertEquals(50000.0, monto);
    }

    @Test
    @DisplayName("PLANA: cobra valor fijo independiente de duracion")
    void plana_valorFijo() {
        Tarifa t = tarifa("PLANA", 5000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(8));
        assertEquals(5000.0, monto);
    }

    @Test
    @DisplayName("Duracion 0 (mismo instante): POR_HORA cobra 0")
    void duracionCero() {
        Tarifa t = tarifa("POR_HORA", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada);
        assertEquals(0.0, monto);
    }

    @Test
    @DisplayName("Salida antes de entrada (clock skew): trata como duracion 0")
    void salidaAntesQueEntrada() {
        Tarifa t = tarifa("POR_HORA", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 9, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.minusMinutes(5));
        assertEquals(0.0, monto);
    }

    @Test
    @DisplayName("Tarifa null lanza BusinessException con codigo ERR_NO_TARIFA")
    void tarifaNull_lanza() {
        Ticket ticket = new Ticket();
        ticket.setFechaHoraEntrada(LocalDateTime.now());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> calculator.calcular(ticket, LocalDateTime.now().plusHours(1)));
        assertEquals("ERR_NO_TARIFA", ex.getErrorCode());
    }

    @Test
    @DisplayName("Unidad desconocida lanza BusinessException")
    void unidadInvalida_lanza() {
        Tarifa t = tarifa("INEXISTENTE", 1000, null);
        Ticket ticket = ticketCon(t, LocalDateTime.now());
        assertThrows(BusinessException.class,
                () -> calculator.calcular(ticket, LocalDateTime.now().plusHours(1)));
    }

    @Test
    @DisplayName("Unidad 'Hora' (sin POR_) se normaliza a POR_HORA")
    void unidad_Hora_seNormaliza() {
        Tarifa t = tarifa("Hora", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(2));
        assertEquals(6000.0, monto);
    }

    @Test
    @DisplayName("Unidad 'hora' minuscula tambien funciona")
    void unidad_hora_minuscula() {
        Tarifa t = tarifa("hora", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(1));
        assertEquals(3000.0, monto);
    }

    @Test
    @DisplayName("Unidad 'Día' con tilde se normaliza a POR_DIA")
    void unidad_DiaConTilde() {
        Tarifa t = tarifa("Día", 25000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(25));
        assertEquals(50000.0, monto);
    }

    @Test
    @DisplayName("Unidad 'Por Hora' con espacio se normaliza")
    void unidad_PorHoraConEspacio() {
        Tarifa t = tarifa("Por Hora", 3000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(1));
        assertEquals(3000.0, monto);
    }

    @Test
    @DisplayName("Unidad 'Fraccion' (sin POR_) se normaliza")
    void unidad_Fraccion() {
        Tarifa t = tarifa("Fraccion", 800, 15);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusMinutes(20));
        assertEquals(1600.0, monto);
    }

    @Test
    @DisplayName("Unidad 'Plana' se normaliza")
    void unidad_Plana() {
        Tarifa t = tarifa("Plana", 5000, null);
        LocalDateTime entrada = LocalDateTime.of(2026, 5, 13, 10, 0);
        double monto = calculator.calcular(ticketCon(t, entrada), entrada.plusHours(8));
        assertEquals(5000.0, monto);
    }
}
