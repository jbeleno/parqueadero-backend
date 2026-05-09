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

        String unidad = tarifa.getUnidad();
        double valor = tarifa.getValor();

        if (unidad == null) {
            throw new BusinessException("Tarifa sin unidad definida", "ERR_TARIFA_UNIDAD");
        }

        switch (unidad) {
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
                        "Unidad de tarifa no soportada: " + unidad,
                        "ERR_TARIFA_UNIDAD_DESCONOCIDA");
        }
    }
}
