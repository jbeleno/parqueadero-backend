package com.usco.parqueaderos_api.convenio.service;

import com.usco.parqueaderos_api.convenio.entity.Convenio;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Aplica el descuento de un convenio sobre un monto base.
 * Reglas:
 * - Si la compra no llega al monto minimo: descuento = 0.
 * - MONTO_FIJO: resta valorDescuento (clamp en 0).
 * - PORCENTAJE: resta monto * porcentaje/100 (clamp en 0).
 * - MINUTOS_GRATIS: el cobro se reduce proporcionalmente a (minutosGratis/totalMin).
 *   Si totalMinutos <= minutosGratis, el descuento es 100% (monto = 0).
 * - Vigencia: si la fecha actual no esta dentro del rango, descuento = 0.
 * - Inactivo: descuento = 0.
 */
@Component
public class ConvenioDescuentoCalculator {

    public double aplicar(Convenio convenio, double montoBase, double montoCompra, long totalMinutos) {
        if (convenio == null || convenio.getActivo() == null || !convenio.getActivo()) return 0.0;
        if (!estaVigente(convenio, LocalDateTime.now())) return 0.0;
        if (convenio.getMontoMinimoCompra() != null && montoCompra < convenio.getMontoMinimoCompra()) {
            return 0.0;
        }
        String tipo = convenio.getTipoDescuento() != null
                ? convenio.getTipoDescuento().toUpperCase() : "";
        return switch (tipo) {
            case "MONTO_FIJO" -> Math.min(montoBase,
                    convenio.getValorDescuento() != null ? convenio.getValorDescuento() : 0.0);
            case "PORCENTAJE" -> {
                double pct = convenio.getPorcentajeDescuento() != null
                        ? convenio.getPorcentajeDescuento() : 0.0;
                yield Math.min(montoBase, montoBase * pct / 100.0);
            }
            case "MINUTOS_GRATIS" -> {
                int min = convenio.getMinutosGratis() != null ? convenio.getMinutosGratis() : 0;
                if (totalMinutos <= 0) yield 0.0;
                if (totalMinutos <= min) yield montoBase;
                yield montoBase * ((double) min / totalMinutos);
            }
            default -> 0.0;
        };
    }

    private boolean estaVigente(Convenio c, LocalDateTime ahora) {
        if (c.getFechaInicioVigencia() != null && ahora.isBefore(c.getFechaInicioVigencia())) return false;
        if (c.getFechaFinVigencia() != null && ahora.isAfter(c.getFechaFinVigencia())) return false;
        return true;
    }
}
