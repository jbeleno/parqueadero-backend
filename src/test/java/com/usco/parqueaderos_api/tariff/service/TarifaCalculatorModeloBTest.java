package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del Modelo B de cobro: gracia + valor minimo que cubre X minutos
 * + tarifa normal sobre los minutos excedentes.
 */
class TarifaCalculatorModeloBTest {

    private TarifaCalculatorService calc;
    private LocalDateTime entrada;

    @BeforeEach
    void setup() {
        calc = new TarifaCalculatorService();
        entrada = LocalDateTime.of(2026, 5, 1, 10, 0);
    }

    private Ticket ticket(Tarifa t, LocalDateTime e) {
        Ticket tk = new Ticket();
        tk.setTarifa(t);
        tk.setFechaHoraEntrada(e);
        return tk;
    }

    private Tarifa tarifa(double valor, String unidad, int gracia, double minimo, int cubreMin) {
        Tarifa t = new Tarifa();
        t.setValor(valor);
        t.setUnidad(unidad);
        t.setMinutosGracia(gracia);
        t.setValorMinimo(minimo);
        t.setMinutosCubiertosPorMinimo(cubreMin);
        return t;
    }

    @Test
    @DisplayName("Estadia dentro de gracia -> 0")
    void gracia_cero_cobro() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        double monto = calc.calcular(tk, entrada.plusMinutes(4));
        assertEquals(0.0, monto, 0.001);
    }

    @Test
    @DisplayName("Estadia justo en el limite de la gracia -> 0")
    void gracia_limite() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        double monto = calc.calcular(tk, entrada.plusMinutes(5));
        assertEquals(0.0, monto, 0.001);
    }

    @Test
    @DisplayName("Estadia entre gracia y minutos_cubiertos -> valor minimo")
    void minimo_cubre() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        // 15 min, dentro de 30 min cubiertos por el minimo
        double monto = calc.calcular(tk, entrada.plusMinutes(15));
        assertEquals(3000.0, monto, 0.001);
    }

    @Test
    @DisplayName("Estadia justo en el limite del cubrimiento -> valor minimo")
    void minimo_limite() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        double monto = calc.calcular(tk, entrada.plusMinutes(30));
        assertEquals(3000.0, monto, 0.001);
    }

    @Test
    @DisplayName("Estadia 60 min con cubrimiento 30 min -> minimo + 30 min normal redondeado a 1 hora")
    void excedente_por_hora() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        // 60 min total = 30 cubiertos + 30 excedentes = 0.5h redondeado a 1h
        // monto = 3000 + 1 * 3000 = 6000
        double monto = calc.calcular(tk, entrada.plusMinutes(60));
        assertEquals(6000.0, monto, 0.001);
    }

    @Test
    @DisplayName("Estadia 90 min con cubrimiento 30 min -> minimo + 60 min normal = 1 hora")
    void excedente_60min() {
        Tarifa t = tarifa(3000, "POR_HORA", 5, 3000, 30);
        Ticket tk = ticket(t, entrada);
        // 90 total = 30 cubiertos + 60 excedentes = 1h
        // monto = 3000 + 1 * 3000 = 6000
        double monto = calc.calcular(tk, entrada.plusMinutes(90));
        assertEquals(6000.0, monto, 0.001);
    }

    @Test
    @DisplayName("Tarifa sin gracia ni minimo -> comportamiento clasico POR_HORA")
    void compat_sin_modelo_b() {
        Tarifa t = tarifa(3000, "POR_HORA", 0, 0, 0);
        Ticket tk = ticket(t, entrada);
        double monto = calc.calcular(tk, entrada.plusMinutes(45));
        assertEquals(3000.0, monto, 0.001); // 1h redondeada
    }

    @Test
    @DisplayName("PLANA con minimo + cubre 60 min -> minimo solo en cubrimiento, plana despues")
    void plana_con_minimo() {
        Tarifa t = tarifa(5000, "PLANA", 0, 2000, 60);
        Ticket tk = ticket(t, entrada);
        // 30 min: dentro de cubrimiento -> minimo = 2000
        assertEquals(2000.0, calc.calcular(tk, entrada.plusMinutes(30)), 0.001);
        // 120 min: minimo + plana = 2000 + 5000 = 7000
        assertEquals(7000.0, calc.calcular(tk, entrada.plusMinutes(120)), 0.001);
    }
}
