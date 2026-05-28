package com.usco.parqueaderos_api.subscription.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.tariff.repository.TarifaRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona el ciclo de vida de las Suscripciones.
 *
 * - Crear (MENSUAL / PASE_DIA / ABONO_PREPAGO) con vigencia calculada.
 * - Resolver la suscripcion ACTIVA aplicable a un vehiculo+parqueadero.
 * - Cancelar.
 * - Marcar como VENCIDA via job @Scheduled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionService {

    private static final int DIAS_MENSUALIDAD = 30;
    private static final int HORAS_PASE_DIA = 24;
    private static final int DIAS_VIGENCIA_ABONO = 365;

    private final SuscripcionRepository suscripcionRepo;
    private final VehiculoRepository vehiculoRepo;
    private final ParqueaderoRepository parqueaderoRepo;
    private final TarifaRepository tarifaRepo;
    private final com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository puntoParqueoRepo;
    private final com.usco.parqueaderos_api.auth.service.CurrentUserService currentUser;
    /** Optional para preservar compat con tests que mocken por @InjectMocks sin DI completo. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.usco.parqueaderos_api.notification.service.NotificationService notificationService;

    /**
     * Crea una suscripcion ACTIVA. Asume que el pago ya fue confirmado por el
     * caller (controller). El monto_pagado se congela en la suscripcion.
     */
    @Transactional
    public Suscripcion crear(Long vehiculoId, Long parqueaderoId, Long tarifaId,
                              TipoSuscripcion tipo, Double montoPagado) {
        return crear(vehiculoId, parqueaderoId, tarifaId, tipo, montoPagado, null);
    }

    /** Sobrecarga: permite reservar un punto especifico para la suscripcion. */
    @Transactional
    @com.usco.parqueaderos_api.audit.aspect.Auditable(tabla = "suscripcion", accion = "CREATE")
    public Suscripcion crear(Long vehiculoId, Long parqueaderoId, Long tarifaId,
                              TipoSuscripcion tipo, Double montoPagado,
                              Long puntoParqueoReservadoId) {
        Vehiculo v = vehiculoRepo.findById(vehiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", vehiculoId));
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        Tarifa t = tarifaRepo.findById(tarifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa", tarifaId));

        // Multi-tenant + assignment: ADMIN_PARQUEADERO solo puede crear suscripciones
        // en sus parqueaderos asignados. ADMIN solo en su empresa.
        if (p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero()) {
            currentUser.requireParqueadero(p.getId());
        }

        // Validar precio configurado en la tarifa
        if (tipo == TipoSuscripcion.MENSUAL && t.getPrecioMensualidad() == null) {
            throw new BusinessException(
                    "La tarifa no tiene precio_mensualidad configurado",
                    "ERR_TARIFA_SIN_MENSUALIDAD");
        }
        if (tipo == TipoSuscripcion.PASE_DIA && t.getPrecioPaseDia() == null) {
            throw new BusinessException(
                    "La tarifa no tiene precio_pase_dia configurado",
                    "ERR_TARIFA_SIN_PASE_DIA");
        }

        // Verificar que no haya otra ACTIVA del mismo tipo
        Optional<Suscripcion> existente = suscripcionRepo
                .findFirstByVehiculoIdAndParqueaderoIdAndTipoAndEstado(
                        vehiculoId, parqueaderoId, tipo, EstadoSuscripcion.ACTIVA);
        if (existente.isPresent()) {
            throw new BusinessException(
                    "El vehiculo ya tiene una suscripcion " + tipo + " ACTIVA en este parqueadero",
                    "ERR_SUSCRIPCION_DUPLICADA");
        }

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fin = calcularFin(inicio, tipo);

        Suscripcion s = new Suscripcion();
        s.setVehiculo(v);
        s.setParqueadero(p);
        s.setTarifa(t);
        s.setTipo(tipo);
        s.setEstado(EstadoSuscripcion.ACTIVA);
        s.setFechaInicio(inicio);
        s.setFechaFin(fin);
        s.setMontoPagado(montoPagado);
        if (tipo == TipoSuscripcion.ABONO_PREPAGO) {
            s.setSaldoRestante(montoPagado); // saldo inicial = monto cargado
        }
        if (puntoParqueoReservadoId != null) {
            com.usco.parqueaderos_api.parking.entity.PuntoParqueo punto =
                    puntoParqueoRepo.findById(puntoParqueoReservadoId)
                            .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", puntoParqueoReservadoId));
            // Validar que el punto sea del mismo parqueadero
            if (punto.getSubSeccion() != null
                    && punto.getSubSeccion().getSeccion() != null
                    && punto.getSubSeccion().getSeccion().getParqueadero() != null
                    && !punto.getSubSeccion().getSeccion().getParqueadero().getId().equals(parqueaderoId)) {
                throw new BusinessException(
                        "El punto reservado no pertenece al parqueadero",
                        "ERR_PUNTO_OTRO_PARQUEADERO");
            }
            // No permitir doble reserva del mismo punto
            Optional<Suscripcion> ya = suscripcionRepo.findActivaByPuntoReservado(puntoParqueoReservadoId);
            if (ya.isPresent()) {
                throw new BusinessException(
                        "El punto ya esta reservado por la suscripcion #" + ya.get().getId(),
                        "ERR_PUNTO_RESERVADO_OCUPADO");
            }
            s.setPuntoParqueoReservado(punto);
        }
        s.setFechaCreacion(inicio);
        try { s.setCreadoPorUsuarioId(currentUser.getCurrentUserId()); } catch (Exception ignored) {}

        try {
            return suscripcionRepo.saveAndFlush(s);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "El vehiculo ya tiene una suscripcion " + tipo + " ACTIVA (detectado por constraint)",
                    "ERR_SUSCRIPCION_DUPLICADA");
        }
    }

    private LocalDateTime calcularFin(LocalDateTime inicio, TipoSuscripcion tipo) {
        switch (tipo) {
            case MENSUAL:        return inicio.plusDays(DIAS_MENSUALIDAD);
            case PASE_DIA:       return inicio.plusHours(HORAS_PASE_DIA);
            case ABONO_PREPAGO:  return inicio.plusDays(DIAS_VIGENCIA_ABONO);
            default: throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        }
    }

    /** Resuelve la suscripcion ACTIVA aplicable, en orden de prelacion MENSUAL > PASE_DIA > ABONO_PREPAGO. */
    @Transactional(readOnly = true)
    public Optional<Suscripcion> findActivaAplicable(Long vehiculoId, Long parqueaderoId) {
        List<Suscripcion> activas = suscripcionRepo
                .findByVehiculoIdAndParqueaderoIdAndEstadoOrderByTipoAsc(
                        vehiculoId, parqueaderoId, EstadoSuscripcion.ACTIVA);
        // ordering por tipo (string): ABONO_PREPAGO, MENSUAL, PASE_DIA — no es la prelacion
        // que quiero. La ordeno explicito en java.
        return activas.stream()
                .sorted((a, b) -> prelacion(a.getTipo()) - prelacion(b.getTipo()))
                .findFirst();
    }

    private int prelacion(TipoSuscripcion tipo) {
        switch (tipo) {
            case MENSUAL:       return 1;
            case PASE_DIA:      return 2;
            case ABONO_PREPAGO: return 3;
            default:            return 99;
        }
    }

    @Transactional
    @com.usco.parqueaderos_api.audit.aspect.Auditable(tabla = "suscripcion", accion = "CANCELAR")
    public Suscripcion cancelar(Long id, boolean reembolsar) {
        Suscripcion s = suscripcionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", id));

        // Multi-tenant + assignment
        if (s.getParqueadero() != null && s.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(s.getParqueadero().getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero() && s.getParqueadero() != null) {
            currentUser.requireParqueadero(s.getParqueadero().getId());
        }

        if (s.getEstado() == EstadoSuscripcion.CANCELADA) return s;

        // Snapshot pre-cambio: si tenia punto reservado, lo notificaremos al
        // front para que actualice estado del punto sin polling.
        Long puntoReservadoId = s.getPuntoParqueoReservado() != null
                ? s.getPuntoParqueoReservado().getId() : null;
        Long parqueaderoId = s.getParqueadero() != null ? s.getParqueadero().getId() : null;

        s.setEstado(EstadoSuscripcion.CANCELADA);
        s.setCanceladoPorUsuarioId(currentUser.getCurrentUserId());
        s.setCanceladoEn(LocalDateTime.now());
        if (reembolsar) {
            log.info("Suscripcion {} cancelada CON reembolso (monto={})", id, s.getMontoPagado());
            // TODO: integrar con servicio de pagos para emitir refund
        } else {
            log.info("Suscripcion {} cancelada SIN reembolso", id);
        }
        Suscripcion saved = suscripcionRepo.save(s);

        // Liberar el punto reservado: emitir evento WebSocket para refresh del front.
        if (puntoReservadoId != null && parqueaderoId != null && notificationService != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("puntoParqueoId", puntoReservadoId);
            data.put("suscripcionId", id);
            data.put("nuevoEstado", "DISPONIBLE");
            data.put("nuevoEstadoLower", "free");
            com.usco.parqueaderos_api.notification.dto.NotificacionDTO n =
                    com.usco.parqueaderos_api.notification.dto.NotificacionDTO.builder()
                            .tipo("SPOT_STATUS_CHANGE")
                            .mensaje("Punto liberado: suscripcion #" + id + " cancelada")
                            .referenciaId(puntoReservadoId)
                            .parqueaderoId(parqueaderoId)
                            .data(data)
                            .build();
            notificationService.notificarParqueadero(parqueaderoId, n);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Suscripcion> findByVehiculo(Long vehiculoId) {
        return suscripcionRepo.findByVehiculoId(vehiculoId);
    }

    @Transactional(readOnly = true)
    public List<Suscripcion> findActivasPorParqueadero(Long parqueaderoId) {
        // Lectura: validar asignacion para roles por-parqueadero
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            currentUser.requireParqueadero(parqueaderoId);
        }
        return suscripcionRepo.findByParqueaderoIdAndEstado(parqueaderoId, EstadoSuscripcion.ACTIVA);
    }

    @Transactional(readOnly = true)
    public Suscripcion findById(Long id) {
        Suscripcion s = suscripcionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion", id));
        if (s.getParqueadero() != null && s.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(s.getParqueadero().getEmpresa().getId());
        }
        if ((currentUser.isAdminParqueadero() || currentUser.isOperarioCaja())
                && s.getParqueadero() != null) {
            currentUser.requireParqueadero(s.getParqueadero().getId());
        }
        return s;
    }
}
