package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calcula el monto a cobrar por un ticket de forma autoritativa,
 * usando reloj del servidor y la unidad declarada en la Tarifa.
 *
 * El frontend NUNCA decide el monto. El backend lo calcula al cerrar
 * el ticket y lo persiste de forma inmutable.
 */
@Service
public class TarifaCalculatorService {

    /**
     * Calcula el monto a cobrar dado un ticket y la fecha/hora de salida.
     * Si la duracion es negativa (reloj atrasado del cliente), se trata como 0.
     *
     * Reglas por unidad:
     * - POR_HORA:     redondeo hacia arriba a horas completas
     * - POR_FRACCION: redondeo hacia arriba en bloques de N minutos
     * - POR_DIA:      redondeo hacia arriba a dias completos (1 dia = 1440 min)
     * - PLANA:        valor fijo independiente de la duracion
     */
    /**
     * Calcula el monto a cobrar dado un ticket y la fecha/hora de salida.
     *
     * MODELO B (con gracia + valor minimo que cubre X minutos):
     *   1. Si la estadia <= minutos_gracia -> 0 (gratis)
     *   2. Si la estadia <= minutos_cubiertos_por_minimo -> valor_minimo
     *   3. Si la estadia es mayor -> valor_minimo + tarifa_normal(excedente)
     *
     * Si la tarifa no usa gracia/minimo (campos = 0), se comporta como antes:
     * cobro directo por unidad.
     */
    public double calcular(Ticket ticket, LocalDateTime salida) {
        if (ticket == null) {
            throw new BusinessException("Ticket nulo en calculo de tarifa", "ERR_TICKET_NULL");
        }
        Tarifa tarifa = ticket.getTarifa();
        if (tarifa == null) {
            throw new BusinessException("El ticket no tiene tarifa asociada", "ERR_NO_TARIFA");
        }
        if (ticket.getFechaHoraEntrada() == null) {
            throw new BusinessException("El ticket no tiene fecha de entrada", "ERR_NO_ENTRADA");
        }
        if (salida == null) {
            throw new BusinessException("Fecha de salida nula en calculo", "ERR_NO_SALIDA");
        }
        long minutos = Math.max(0L, Duration.between(ticket.getFechaHoraEntrada(), salida).toMinutes());

        int gracia = tarifa.getMinutosGracia() != null ? tarifa.getMinutosGracia() : 0;
        double minimo = tarifa.getValorMinimo() != null ? tarifa.getValorMinimo() : 0.0;
        int cubreMin = tarifa.getMinutosCubiertosPorMinimo() != null
                ? tarifa.getMinutosCubiertosPorMinimo() : 0;

        // Paso 1: gracia
        if (minutos <= gracia) return 0.0;

        // Paso 2: ventana cubierta por el valor minimo
        if (minutos <= cubreMin) {
            return minimo;
        }

        // Paso 3: minimo + tarifa normal sobre los minutos EXCEDENTES
        long minutosExcedentes = minutos - cubreMin;
        double cobroExcedente = calcularPorUnidad(tarifa, minutosExcedentes);
        return minimo + cobroExcedente;
    }

    /** Calcula cobro segun la unidad de la tarifa para una cantidad de minutos. */
    private double calcularPorUnidad(Tarifa tarifa, long minutos) {
        String unidadNormalizada = normalizarUnidad(tarifa.getUnidad());
        double valor = tarifa.getValor();
        switch (unidadNormalizada) {
            case "POR_HORA":
                return Math.ceil(minutos / 60.0) * valor;
            case "POR_FRACCION":
                int frac = tarifa.getMinutosFraccion() != null && tarifa.getMinutosFraccion() > 0
                        ? tarifa.getMinutosFraccion() : 60;
                return Math.ceil((double) minutos / frac) * valor;
            case "POR_DIA":
                return Math.ceil(minutos / 1440.0) * valor;
            case "PLANA":
                return valor;
            default:
                throw new BusinessException(
                        "Unidad de tarifa no soportada: " + tarifa.getUnidad(),
                        "ERR_TARIFA_UNIDAD_DESCONOCIDA");
        }
    }

    /**
     * Normaliza la unidad de tarifa a uno de los valores canónicos:
     * POR_HORA, POR_FRACCION, POR_DIA, PLANA.
     *
     * Tolerante a variantes que el frontend o usuarios admin puedan
     * haber guardado: "Hora", "hora", "HOUR", "Por Hora", "Día", etc.
     * Case-insensitive, ignora acentos y separadores comunes.
     */
    private String normalizarUnidad(String unidad) {
        if (unidad == null) {
            throw new BusinessException("Tarifa sin unidad definida", "ERR_TARIFA_UNIDAD");
        }
        // Normalizar: minusculas, quitar tildes, quitar espacios y guiones
        String n = unidad.trim().toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replace(" ", "").replace("_", "").replace("-", "");

        // Hora: "hora", "porhora", "hour", "byhour"
        if (n.equals("hora") || n.equals("porhora") || n.equals("hour") || n.equals("byhour")) {
            return "POR_HORA";
        }
        // Fraccion: "fraccion", "porfraccion", "fraction", "byfraction"
        if (n.equals("fraccion") || n.equals("porfraccion") || n.equals("fraction") || n.equals("byfraction")) {
            return "POR_FRACCION";
        }
        // Dia: "dia", "pordia", "day", "byday"
        if (n.equals("dia") || n.equals("pordia") || n.equals("day") || n.equals("byday")) {
            return "POR_DIA";
        }
        // Plana: "plana", "plano", "flat", "fija", "fijo"
        if (n.equals("plana") || n.equals("plano") || n.equals("flat") || n.equals("fija") || n.equals("fijo")) {
            return "PLANA";
        }
        return ""; // hara fallar el switch → ERR_TARIFA_UNIDAD_DESCONOCIDA
    }
}
