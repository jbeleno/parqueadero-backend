package com.usco.parqueaderos_api.ticket.service.strategy;

import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/** Si el vehiculo tiene Suscripcion PASE_DIA activa, el ticket se cubre con monto=0. */
@Component
@Order(2)
@RequiredArgsConstructor
public class PaseDiaCobroStrategy implements CobroStrategy {

    private final SuscripcionRepository suscripcionRepo;

    @Override
    public Optional<CobroResult> intentarCobrar(Ticket ticket, LocalDateTime salida) {
        if (ticket.getVehiculo() == null || ticket.getParqueadero() == null) return Optional.empty();
        Optional<Suscripcion> pase = suscripcionRepo
                .findFirstByVehiculoIdAndParqueaderoIdAndTipoAndEstado(
                        ticket.getVehiculo().getId(),
                        ticket.getParqueadero().getId(),
                        TipoSuscripcion.PASE_DIA,
                        EstadoSuscripcion.ACTIVA);
        return pase
                .filter(s -> salida.isBefore(s.getFechaFin()) || salida.isEqual(s.getFechaFin()))
                .map(s -> new CobroResult(
                        0.0,
                        s.getId(),
                        false,
                        "Cubierto por pase de dia (suscripcion #" + s.getId() + ")"
                ));
    }
}
