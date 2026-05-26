package com.usco.parqueaderos_api.tariff.service;

import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.entity.TarifaFranja;
import com.usco.parqueaderos_api.tariff.repository.TarifaFranjaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Decide si una hora de entrada cae dentro de alguna franja horaria
 * configurada para la tarifa. Si si, devuelve el valor por minuto/hora
 * que aplica a esa franja; si no, NULL (se usa el valor base).
 */
@Service
@RequiredArgsConstructor
public class TarifaFranjaSelector {

    private final TarifaFranjaRepository franjaRepository;

    /** Devuelve la primera franja activa que aplica para la hora de entrada dada. */
    @Transactional(readOnly = true)
    public Optional<TarifaFranja> seleccionar(Tarifa tarifa, LocalDateTime entrada) {
        if (tarifa == null || entrada == null) return Optional.empty();
        List<TarifaFranja> franjas = franjaRepository.findByTarifaIdAndActivaTrue(tarifa.getId());
        boolean finDeSemana = isFinDeSemana(entrada);
        LocalTime hora = entrada.toLocalTime();
        return franjas.stream()
                .filter(f -> !f.getSoloFinesDeSemana() || finDeSemana)
                .filter(f -> aplicaHora(f, hora))
                .findFirst();
    }

    private boolean isFinDeSemana(LocalDateTime dt) {
        DayOfWeek d = dt.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /**
     * Si horaFin > horaInicio: rango normal (ej. 06:00..18:00).
     * Si horaFin <= horaInicio: cruza medianoche (ej. 22:00..06:00).
     */
    private boolean aplicaHora(TarifaFranja f, LocalTime h) {
        LocalTime ini = f.getHoraInicio();
        LocalTime fin = f.getHoraFin();
        if (fin.isAfter(ini)) {
            return !h.isBefore(ini) && h.isBefore(fin);
        }
        return !h.isBefore(ini) || h.isBefore(fin);
    }
}
