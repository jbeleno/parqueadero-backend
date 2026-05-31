package com.usco.parqueaderos_api.caja.service;

import com.usco.parqueaderos_api.audit.service.AuditService;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.caja.entity.Caja;
import com.usco.parqueaderos_api.caja.entity.MovimientoCaja;
import com.usco.parqueaderos_api.caja.entity.TipoMovimientoCaja;
import com.usco.parqueaderos_api.caja.repository.CajaRepository;
import com.usco.parqueaderos_api.caja.repository.MovimientoCajaRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.user.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestion de cajas operativas (G13).
 *
 * Reglas clave:
 *   - Un usuario solo puede tener 1 caja ABIERTA a la vez (UNIQUE INDEX parcial).
 *   - Solo OPERARIO_CAJA / ADMIN / ADMIN_PARQUEADERO / SUPER_ADMIN abren caja.
 *   - El operador debe operar en un parqueadero al que esta asignado (CurrentUserService.requireParqueadero).
 *   - Retiros/depositos: ADMIN_PARQUEADERO / ADMIN / SUPER_ADMIN.
 *   - Ajustes manuales: SUPER_ADMIN solamente.
 *   - Una caja CERRADA es inmutable.
 *   - Todos los movimientos quedan en audit_log + tabla movimiento_caja.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    // ── Abrir ────────────────────────────────────────────────────────────

    @Transactional
    public Caja abrir(Long parqueaderoId, Double fondoInicial, String nombre, String observaciones) {
        if (currentUser.isUser()) {
            throw new AccessDeniedException("USER no puede abrir caja");
        }
        if (fondoInicial == null || fondoInicial < 0) {
            throw new BusinessException("fondoInicial debe ser >= 0", "ERR_INVALID_AMOUNT");
        }
        Usuario u = currentUser.getCurrent();
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        // ADMIN: tambien debe ser parqueadero de su empresa (multi-tenant)
        if (currentUser.isAdmin() && !currentUser.isSuperAdmin()
                && p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }
        // OPERARIO_CAJA / ADMIN_PARQUEADERO: parqueadero debe estar en su asignacion
        if (currentUser.isOperarioCaja() || currentUser.isAdminParqueadero()) {
            currentUser.requireParqueadero(parqueaderoId);
        }

        // Verificar que no tenga ya una abierta
        Optional<Caja> abierta = cajaRepository.findFirstByUsuarioIdAndEstado(u.getId(), "ABIERTA");
        if (abierta.isPresent()) {
            throw new BusinessException(
                    "Ya tienes una caja ABIERTA (id=" + abierta.get().getId() + "). Cierrala antes.",
                    "ERR_CAJA_YA_ABIERTA");
        }

        Caja c = new Caja();
        c.setUsuario(u);
        c.setParqueadero(p);
        c.setNombre(nombre);
        c.setFondoInicial(fondoInicial);
        c.setSaldoCalculado(fondoInicial);
        c.setEstado("ABIERTA");
        c.setAbiertaEn(LocalDateTime.now());
        c.setObservacionesApertura(observaciones);

        try {
            Caja saved = cajaRepository.saveAndFlush(c);
            registrarMovimiento(saved, TipoMovimientoCaja.APERTURA, fondoInicial,
                    "Apertura de caja con fondo $" + fondoInicial, null, u, fondoInicial);
            audit.log("caja", saved.getId(), "ABRIR", null, saved,
                    p.getEmpresa() != null ? p.getEmpresa().getId() : null);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "No puedes abrir 2 cajas a la vez (race condition detectado)",
                    "ERR_CAJA_YA_ABIERTA");
        }
    }

    // ── Cerrar (arqueo) ──────────────────────────────────────────────────

    @Transactional
    public Caja cerrar(Long cajaId, Double saldoContado, String observaciones) {
        Caja c = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja", cajaId));
        if (!"ABIERTA".equals(c.getEstado())) {
            throw new BusinessException("La caja ya esta " + c.getEstado(), "ERR_CAJA_NO_ABIERTA");
        }
        Usuario u = currentUser.getCurrent();
        // Solo el dueno o admin pueden cerrar
        boolean esDueno = c.getUsuario() != null && c.getUsuario().getId().equals(u.getId());
        if (!esDueno && !currentUser.isAdmin() && !currentUser.isSuperAdmin()
                && !currentUser.isAdminParqueadero()) {
            throw new AccessDeniedException("Solo el cajero o admin pueden cerrar la caja");
        }
        if (saldoContado == null || saldoContado < 0) {
            throw new BusinessException("saldoContado >= 0 obligatorio", "ERR_INVALID_AMOUNT");
        }
        double diferencia = saldoContado - c.getSaldoCalculado();

        c.setSaldoContado(saldoContado);
        c.setDiferencia(diferencia);
        c.setEstado("CERRADA");
        c.setCerradaEn(LocalDateTime.now());
        c.setObservacionesCierre(observaciones);
        Caja saved = cajaRepository.save(c);

        registrarMovimiento(saved, TipoMovimientoCaja.CIERRE, 0.0,
                "Cierre. Calculado=" + c.getSaldoCalculado() + " contado=" + saldoContado
                        + " diferencia=" + diferencia, null, u, saldoContado);
        audit.log("caja", saved.getId(), "CERRAR", null, saved,
                c.getParqueadero() != null && c.getParqueadero().getEmpresa() != null
                        ? c.getParqueadero().getEmpresa().getId() : null);
        return saved;
    }

    // ── Retiro / Deposito ────────────────────────────────────────────────

    @Transactional
    public MovimientoCaja retirar(Long cajaId, Double monto, String motivo) {
        return movimientoManual(cajaId, monto, motivo, TipoMovimientoCaja.RETIRO, -1);
    }

    @Transactional
    public MovimientoCaja depositar(Long cajaId, Double monto, String motivo) {
        return movimientoManual(cajaId, monto, motivo, TipoMovimientoCaja.DEPOSITO, +1);
    }

    private MovimientoCaja movimientoManual(Long cajaId, Double monto, String motivo,
                                             TipoMovimientoCaja tipo, int signo) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()
                && !currentUser.isAdminParqueadero()) {
            throw new AccessDeniedException("Solo admin puede registrar " + tipo);
        }
        if (monto == null || monto <= 0) {
            throw new BusinessException("monto > 0 obligatorio", "ERR_INVALID_AMOUNT");
        }
        if (motivo == null || motivo.trim().length() < com.usco.parqueaderos_api.common.validation.MotivoValidator.DEFAULT_MIN_CHARS) {
            throw new BusinessException("motivo obligatorio (min 10 chars)", "ERR_MISSING_FIELDS");
        }
        Caja c = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja", cajaId));
        if (!"ABIERTA".equals(c.getEstado())) {
            throw new BusinessException("Caja no esta ABIERTA", "ERR_CAJA_NO_ABIERTA");
        }
        double delta = signo * monto;
        double nuevo = c.getSaldoCalculado() + delta;
        if (nuevo < 0) {
            throw new BusinessException(
                    "Monto " + monto + " excede el saldo actual " + c.getSaldoCalculado(),
                    "ERR_MONTO_EXCEDE_SALDO");
        }
        c.setSaldoCalculado(nuevo);
        cajaRepository.save(c);
        MovimientoCaja m = registrarMovimiento(c, tipo, delta, motivo, null,
                currentUser.getCurrent(), nuevo);
        audit.log("movimiento_caja", m.getId(), tipo.name(), null, m,
                c.getParqueadero() != null && c.getParqueadero().getEmpresa() != null
                        ? c.getParqueadero().getEmpresa().getId() : null);
        return m;
    }

    // ── Ajuste (SUPER_ADMIN) ─────────────────────────────────────────────

    @Transactional
    public MovimientoCaja ajustar(Long cajaId, Double monto, String motivo) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede ajustar caja");
        }
        if (motivo == null || motivo.trim().length() < com.usco.parqueaderos_api.common.validation.MotivoValidator.DEFAULT_MIN_CHARS) {
            throw new BusinessException("motivo de ajuste obligatorio (min 10)", "ERR_MISSING_FIELDS");
        }
        Caja c = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja", cajaId));
        double nuevo = c.getSaldoCalculado() + monto; // monto puede ser +/-
        c.setSaldoCalculado(nuevo);
        cajaRepository.save(c);
        MovimientoCaja m = registrarMovimiento(c, TipoMovimientoCaja.AJUSTE, monto, motivo, null,
                currentUser.getCurrent(), nuevo);
        audit.log("movimiento_caja", m.getId(), "AJUSTE", null, m,
                c.getParqueadero() != null && c.getParqueadero().getEmpresa() != null
                        ? c.getParqueadero().getEmpresa().getId() : null);
        return m;
    }

    // ── Auto-ingreso de pago EFECTIVO (llamado por listener) ─────────────

    @Transactional
    public Optional<MovimientoCaja> ingresarPagoEfectivo(Pago pago, Usuario usuario) {
        if (pago == null || usuario == null) return Optional.empty();
        if (!"EFECTIVO".equals(pago.getMetodo())) return Optional.empty();
        if (!"COMPLETADO".equals(pago.getEstado())) return Optional.empty();
        Optional<Caja> cajaAbierta = cajaRepository.findFirstByUsuarioIdAndEstado(usuario.getId(), "ABIERTA");
        if (cajaAbierta.isEmpty()) {
            log.warn("Pago EFECTIVO {} de usuario {} sin caja ABIERTA. No se registra movimiento.",
                    pago.getId(), usuario.getId());
            return Optional.empty();
        }
        Caja c = cajaAbierta.get();
        double nuevo = c.getSaldoCalculado() + pago.getMonto();
        c.setSaldoCalculado(nuevo);
        cajaRepository.save(c);
        MovimientoCaja m = registrarMovimiento(c, TipoMovimientoCaja.INGRESO_PAGO,
                pago.getMonto(),
                "Pago factura #" + (pago.getFactura() != null ? pago.getFactura().getId() : "?"),
                pago, usuario, nuevo);
        return Optional.of(m);
    }

    // ── Lecturas ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Caja> miCajaAbierta() {
        return cajaRepository.findFirstByUsuarioIdAndEstado(
                currentUser.getCurrentUserId(), "ABIERTA");
    }

    @Transactional(readOnly = true)
    public List<MovimientoCaja> movimientos(Long cajaId) {
        return movimientoRepository.findByCajaIdOrderByFechaHoraAsc(cajaId);
    }

    @Transactional(readOnly = true)
    public Caja findById(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja", id));
    }

    @Transactional(readOnly = true)
    public List<Caja> listarPorParqueadero(Long parqueaderoId) {
        currentUser.requireParqueadero(parqueaderoId);
        return cajaRepository.findByParqueaderoIdOrderByAbiertaEnDesc(parqueaderoId);
    }

    // ── Helper interno ───────────────────────────────────────────────────

    private MovimientoCaja registrarMovimiento(Caja caja, TipoMovimientoCaja tipo, Double monto,
                                                String motivo, Pago pago, Usuario usuario,
                                                Double saldoResultante) {
        MovimientoCaja m = new MovimientoCaja();
        m.setCaja(caja);
        m.setTipo(tipo);
        m.setMonto(monto);
        m.setPago(pago);
        m.setMotivo(motivo);
        m.setUsuario(usuario);
        m.setSaldoResultante(saldoResultante);
        m.setFechaHora(LocalDateTime.now());
        return movimientoRepository.save(m);
    }
}
