package com.usco.parqueaderos_api.subscription.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.MovimientoSaldo;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.MovimientoSaldoRepository;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona los abonos y consumos de saldo en Suscripciones tipo ABONO_PREPAGO.
 *
 * Cada operacion genera un MovimientoSaldo inmutable (Event Sourcing).
 * Concurrencia protegida con @Version en Suscripcion + retry automatico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoService {

    private final SuscripcionRepository suscripcionRepo;
    private final MovimientoSaldoRepository movimientoRepo;

    /**
     * Carga saldo a una suscripcion ABONO_PREPAGO existente, o crea una nueva.
     * Retorna el movimiento generado (con saldo_resultante actualizado).
     */
    @Transactional
    public MovimientoSaldo abonar(Suscripcion suscripcion, double monto, String motivo) {
        if (suscripcion.getTipo() != TipoSuscripcion.ABONO_PREPAGO) {
            throw new BusinessException(
                    "Solo se puede abonar a suscripciones ABONO_PREPAGO",
                    "ERR_TIPO_NO_SOPORTA_ABONO");
        }
        if (monto <= 0) {
            throw new BusinessException("Monto a abonar debe ser positivo", "ERR_MONTO_INVALIDO");
        }
        double saldoActual = suscripcion.getSaldoRestante() != null
                ? suscripcion.getSaldoRestante() : 0.0;
        double nuevoSaldo = saldoActual + monto;
        suscripcion.setSaldoRestante(nuevoSaldo);
        if (suscripcion.getEstado() == EstadoSuscripcion.AGOTADA) {
            // Re-activa si estaba agotada
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        }
        suscripcionRepo.save(suscripcion);

        MovimientoSaldo m = new MovimientoSaldo();
        m.setSuscripcion(suscripcion);
        m.setMonto(monto);
        m.setSaldoResultante(nuevoSaldo);
        m.setMotivo(motivo != null ? motivo : "Abono manual");
        m.setFecha(LocalDateTime.now());
        return movimientoRepo.save(m);
    }

    /**
     * Descuenta saldo por un consumo (ticket). Retorna el monto efectivamente
     * descontado, que puede ser menor al solicitado si el saldo no alcanza.
     *
     * Si saldo queda en 0, marca la suscripcion como AGOTADA.
     */
    /**
     * Usa lock pesimista (findByIdForUpdate) que serializa los descuentos.
     * No requiere retry porque el segundo thread espera al primero. La proteccion
     * con @Version es defensa adicional en caso de bypass del lock.
     */
    @Transactional
    public DescuentoResult descontar(Long suscripcionId, double montoSolicitado, Ticket ticket) {
        if (montoSolicitado <= 0) {
            return new DescuentoResult(0.0, 0.0, false);
        }
        Suscripcion s = suscripcionRepo.findByIdForUpdate(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", suscripcionId));
        if (s.getTipo() != TipoSuscripcion.ABONO_PREPAGO) {
            throw new BusinessException(
                    "Solo se puede descontar de ABONO_PREPAGO",
                    "ERR_TIPO_NO_SOPORTA_DESCUENTO");
        }
        double saldoActual = s.getSaldoRestante() != null ? s.getSaldoRestante() : 0.0;
        double descontado = Math.min(saldoActual, montoSolicitado);
        double nuevoSaldo = saldoActual - descontado;
        s.setSaldoRestante(nuevoSaldo);
        boolean agotado = nuevoSaldo <= 0.0001;
        if (agotado) {
            s.setEstado(EstadoSuscripcion.AGOTADA);
        }
        suscripcionRepo.save(s);

        MovimientoSaldo m = new MovimientoSaldo();
        m.setSuscripcion(s);
        m.setMonto(-descontado);
        m.setSaldoResultante(nuevoSaldo);
        m.setTicket(ticket);
        m.setMotivo("Consumo ticket #" + (ticket != null ? ticket.getId() : "?"));
        m.setFecha(LocalDateTime.now());
        movimientoRepo.save(m);

        double pendiente = montoSolicitado - descontado;
        log.info("Descuento suscripcion {}: solicitado={} descontado={} pendiente={} saldo_post={}",
                suscripcionId, montoSolicitado, descontado, pendiente, nuevoSaldo);
        return new DescuentoResult(descontado, pendiente, agotado);
    }

    @Transactional(readOnly = true)
    public List<MovimientoSaldo> historial(Long suscripcionId) {
        return movimientoRepo.findBySuscripcionIdOrderByFechaDesc(suscripcionId);
    }

    /** Resultado del descuento de saldo. */
    public record DescuentoResult(double descontado, double pendiente, boolean agotado) {}
}
