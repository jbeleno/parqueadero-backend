package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Modo REEMPLAZO: el valorMinimo solo cubre estadias cortas. Pasado
 * minutosCubiertosPorMinimo, se cobra la TARIFA NORMAL COMPLETA en su lugar
 * (NO se suma al minimo). Reproduce el caso de uso reportado por el cliente:
 *
 *   valorMinimo=2000, cubre=5, valor=3000/hora, gracia=0
 *     <=5 min  -> $2000 (minimo)
 *     6-60 min -> $3000 (1 hora normal, reemplaza al minimo)
 *    61-120 min -> $6000 (2 horas)
 */
class TarifaCalculatorModoReemplazoTest {

    private TarifaCalculatorService calc;
    private LocalDateTime entrada;

    @BeforeEach
    void setup() {
        calc = new TarifaCalculatorService();
        entrada = LocalDateTime.of(2026, 5, 1, 10, 0);
    }

    private Tarifa tarifa(double valor, int gracia, double minimo, int cubre, boolean reemplaza) {
        Tarifa t = new Tarifa();
        t.setValor(valor);
        t.setUnidad("POR_HORA");
        t.setMinutosGracia(gracia);
        t.setValorMinimo(minimo);
        t.setMinutosCubiertosPorMinimo(cubre);
        t.setValorMinimoReemplaza(reemplaza);
        return t;
    }

    private Ticket ticket(Tarifa t) {
        Ticket tk = new Ticket();
        tk.setTarifa(t);
        tk.setFechaHoraEntrada(entrada);
        return tk;
    }

    @Test
    @DisplayName("Caso cliente: 2min con cubre=5 -> $2000 (minimo)")
    void corta_dentro_de_cubrimiento() {
        Tarifa t = tarifa(3000, 0, 2000, 5, true);
        Ticket tk = ticket(t);
        assertEquals(2000.0, calc.calcular(tk, entrada.plusMinutes(2)), 0.001);
    }

    @Test
    @DisplayName("Caso cliente: 5min justo en el limite -> $2000")
    void en_el_limite() {
        Tarifa t = tarifa(3000, 0, 2000, 5, true);
        assertEquals(2000.0, calc.calcular(ticket(t), entrada.plusMinutes(5)), 0.001);
    }

    @Test
    @DisplayName("Caso cliente: 30min -> tarifa normal $3000 (NO $2000+$3000)")
    void media_hora_reemplaza_minimo() {
        Tarifa t = tarifa(3000, 0, 2000, 5, true);
        assertEquals(3000.0, calc.calcular(ticket(t), entrada.plusMinutes(30)), 0.001);
    }

    @Test
    @DisplayName("Caso cliente: 60min -> $3000 (1 hora exacta)")
    void una_hora() {
        Tarifa t = tarifa(3000, 0, 2000, 5, true);
        assertEquals(3000.0, calc.calcular(ticket(t), entrada.plusMinutes(60)), 0.001);
    }

    @Test
    @DisplayName("Caso cliente: 120min -> $6000 (2 horas, sin sumar minimo)")
    void dos_horas() {
        Tarifa t = tarifa(3000, 0, 2000, 5, true);
        assertEquals(6000.0, calc.calcular(ticket(t), entrada.plusMinutes(120)), 0.001);
    }

    @Test
    @DisplayName("Modo aditivo (default): mismo escenario suma minimo + excedente")
    void aditivo_compat() {
        Tarifa t = tarifa(3000, 0, 2000, 5, false);
        // 30 min: 2000 + ceil(25/60)*3000 = 2000 + 3000 = 5000
        assertEquals(5000.0, calc.calcular(ticket(t), entrada.plusMinutes(30)), 0.001);
        // 120 min: 2000 + ceil(115/60)*3000 = 2000 + 6000 = 8000
        assertEquals(8000.0, calc.calcular(ticket(t), entrada.plusMinutes(120)), 0.001);
    }
}
