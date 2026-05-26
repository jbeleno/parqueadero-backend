package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Valida cumplimiento de NTC 4904 (norma colombiana): minimo 2% de
 * puntos reservados para personas con discapacidad. Reporta el estado
 * pero NO bloquea operaciones; el operador queda informado.
 */
@Service
@RequiredArgsConstructor
public class Ntc4904ValidatorService {

    private static final double PORCENTAJE_MINIMO = 0.02; // 2%

    private final PuntoParqueoRepository puntoRepository;

    public record ResultadoNtc4904(
            int totalPuntos,
            int puntosDiscapacitados,
            int requeridosMinimo,
            double porcentajeActual,
            boolean cumple,
            String mensaje
    ) {}

    @Transactional(readOnly = true)
    public ResultadoNtc4904 evaluar(Long parqueaderoId) {
        List<PuntoParqueo> puntos = puntoRepository.findActiveByParqueaderoId(parqueaderoId);
        int total = puntos.size();
        int discap = (int) puntos.stream()
                .filter(p -> p.getTipoPuntoParqueo() != null
                        && p.getTipoPuntoParqueo().getNombre() != null
                        && p.getTipoPuntoParqueo().getNombre().toLowerCase().contains("discapacit"))
                .count();
        int requeridos = (int) Math.ceil(total * PORCENTAJE_MINIMO);
        double pct = total == 0 ? 0.0 : ((double) discap) / total;
        boolean cumple = discap >= requeridos;
        String mensaje = cumple
                ? "Cumple NTC 4904: " + discap + "/" + total + " (" + String.format("%.1f%%", pct * 100) + ")"
                : "NO cumple NTC 4904: requiere " + requeridos + ", tiene " + discap;
        return new ResultadoNtc4904(total, discap, requeridos, pct, cumple, mensaje);
    }
}
