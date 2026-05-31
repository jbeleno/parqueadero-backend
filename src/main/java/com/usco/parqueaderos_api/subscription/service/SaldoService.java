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
    /** Optional: usar SecurityContext si esta presente para auto-popular registrado_por. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.usco.parqueaderos_api.auth.service.CurrentUserService currentUser;

    private Long currentUserIdOrNull() {
        try {
            return currentUser != null ? currentUser.getCurrentUserId() : null;
        } catch (Exception e) { return null; }
    }

    /**
     * Carga saldo a una suscripcion ABONO_PREPAGO existente, o crea una nueva.
     * Retorna el movimiento generado (con saldo_resultante actualizado).
     */
    @Transactional
    @com.usco.parqueaderos_api.audit.aspect.Auditable(tabla = "movimiento_saldo", accion = "ABONO")
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
        m.setTipo(com.usco.parqueaderos_api.subscription.entity.TipoMovimiento.ABONO);
        m.setMotivo(motivo != null ? motivo : "Abono manual");
        m.setFecha(LocalDateTime.now());
        m.setRegistradoPorUsuarioId(currentUserIdOrNull());
        return movimientoRepo.save(m);
    }

    /**
     * Ajuste manual del operador (SUPER_ADMIN). monto puede ser positivo o
     * negativo. NO toca estado AGOTADA -> es responsabilidad del operador.
     */
    @Transactional
    @com.usco.parqueaderos_api.audit.aspect.Auditable(tabla = "movimiento_saldo", accion = "AJUSTE", requiereMotivo = true)
    public MovimientoSaldo ajustar(Long suscripcionId, double monto, String motivo) {
        if (motivo == null || motivo.trim().length() < com.usco.parqueaderos_api.common.validation.MotivoValidator.DEFAULT_MIN_CHARS) {
            throw new BusinessException(
                    "El motivo de ajuste es obligatorio (min 10 caracteres)",
                    "ERR_MISSING_FIELDS");
        }
        Suscripcion s = suscripcionRepo.findByIdForUpdate(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", suscripcionId));
        double saldoActual = s.getSaldoRestante() != null ? s.getSaldoRestante() : 0.0;
        double nuevoSaldo = saldoActual + monto;
        if (nuevoSaldo < 0) {
            throw new BusinessException(
                    "El ajuste dejaria saldo negativo (" + nuevoSaldo + ")",
                    "ERR_INVALID_AMOUNT");
        }
        s.setSaldoRestante(nuevoSaldo);
        suscripcionRepo.save(s);
        MovimientoSaldo m = new MovimientoSaldo();
        m.setSuscripcion(s);
        m.setMonto(monto);
        m.setSaldoResultante(nuevoSaldo);
        m.setTipo(com.usco.parqueaderos_api.subscription.entity.TipoMovimiento.AJUSTE);
        m.setMotivo(motivo);
        m.setFecha(LocalDateTime.now());
        m.setRegistradoPorUsuarioId(currentUserIdOrNull());
        return movimientoRepo.save(m);
    }

    /**
     * Reversa un consumo previo (cuando se anula el ticket que descontó).
     * Devuelve el monto al saldo.
     */
    @Transactional
    @com.usco.parqueaderos_api.audit.aspect.Auditable(tabla = "movimiento_saldo", accion = "REVERSO")
    public MovimientoSaldo reversar(Long suscripcionId, double montoARevertir, Long ticketId) {
        Suscripcion s = suscripcionRepo.findByIdForUpdate(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", suscripcionId));
        double saldoActual = s.getSaldoRestante() != null ? s.getSaldoRestante() : 0.0;
        double nuevoSaldo = saldoActual + Math.abs(montoARevertir);
        s.setSaldoRestante(nuevoSaldo);
        // Si estaba AGOTADA y entra saldo, volver a ACTIVA
        if (s.getEstado() == EstadoSuscripcion.AGOTADA) {
            s.setEstado(EstadoSuscripcion.ACTIVA);
        }
        suscripcionRepo.save(s);
        MovimientoSaldo m = new MovimientoSaldo();
        m.setSuscripcion(s);
        m.setMonto(Math.abs(montoARevertir));
        m.setSaldoResultante(nuevoSaldo);
        m.setTipo(com.usco.parqueaderos_api.subscription.entity.TipoMovimiento.REVERSO);
        m.setMotivo("Reverso por anulacion ticket #" + ticketId);
        m.setFecha(LocalDateTime.now());
        m.setRegistradoPorUsuarioId(currentUserIdOrNull());
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
        m.setTipo(com.usco.parqueaderos_api.subscription.entity.TipoMovimiento.CONSUMO);
        m.setMotivo("Consumo ticket #" + (ticket != null ? ticket.getId() : "?"));
        m.setFecha(LocalDateTime.now());
        m.setRegistradoPorUsuarioId(currentUserIdOrNull());
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
