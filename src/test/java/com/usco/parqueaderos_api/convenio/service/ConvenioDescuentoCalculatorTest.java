package com.usco.parqueaderos_api.convenio.service;

import com.usco.parqueaderos_api.convenio.entity.Convenio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConvenioDescuentoCalculatorTest {

    private final ConvenioDescuentoCalculator calc = new ConvenioDescuentoCalculator();

    private Convenio base(String tipo) {
        Convenio c = new Convenio();
        c.setTipoDescuento(tipo);
        c.setActivo(true);
        return c;
    }

    @Test
    @DisplayName("MONTO_FIJO descuenta el valor, sin pasar el monto base")
    void monto_fijo() {
        Convenio c = base("MONTO_FIJO");
        c.setValorDescuento(3000.0);
        assertEquals(3000.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
        // descuento mayor al monto: clamp al monto base
        c.setValorDescuento(99999.0);
        assertEquals(5000.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("PORCENTAJE descuenta % sobre el monto")
    void porcentaje() {
        Convenio c = base("PORCENTAJE");
        c.setPorcentajeDescuento(50.0);
        assertEquals(2500.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
        c.setPorcentajeDescuento(100.0);
        assertEquals(5000.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("MINUTOS_GRATIS descuenta proporcionalmente; 100% si totalMin<=minutos")
    void minutos_gratis() {
        Convenio c = base("MINUTOS_GRATIS");
        c.setMinutosGratis(60);
        // 120 min, 60 gratis -> 50% descuento
        assertEquals(5000.0 * 0.5, calc.aplicar(c, 5000.0, 10000.0, 120), 0.001);
        // 30 min, 60 gratis -> 100% descuento
        assertEquals(5000.0, calc.aplicar(c, 5000.0, 10000.0, 30), 0.001);
        // 60 min, 60 gratis -> 100% descuento (limite)
        assertEquals(5000.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("Si monto compra < monto minimo, descuento = 0")
    void monto_minimo_no_alcanzado() {
        Convenio c = base("MONTO_FIJO");
        c.setValorDescuento(3000.0);
        c.setMontoMinimoCompra(50000.0);
        assertEquals(0.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("Convenio inactivo -> descuento 0")
    void inactivo() {
        Convenio c = base("PORCENTAJE");
        c.setPorcentajeDescuento(50.0);
        c.setActivo(false);
        assertEquals(0.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("Fuera de vigencia -> descuento 0")
    void fuera_de_vigencia() {
        Convenio c = base("PORCENTAJE");
        c.setPorcentajeDescuento(50.0);
        c.setFechaInicioVigencia(LocalDateTime.now().plusDays(1));
        assertEquals(0.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
        // expirado
        c.setFechaInicioVigencia(null);
        c.setFechaFinVigencia(LocalDateTime.now().minusDays(1));
        assertEquals(0.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }

    @Test
    @DisplayName("Convenio nulo o sin tipo -> descuento 0")
    void edge_cases() {
        assertEquals(0.0, calc.aplicar(null, 5000.0, 10000.0, 60), 0.001);
        Convenio c = new Convenio();
        c.setActivo(true);
        c.setTipoDescuento(null);
        assertEquals(0.0, calc.aplicar(c, 5000.0, 10000.0, 60), 0.001);
    }
}
