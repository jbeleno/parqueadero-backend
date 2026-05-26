package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.PagoDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.MetodoPago;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final double TOLERANCIA_DECIMAL = 0.01;

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<PagoDTO> findAll() {
        List<Pago> base;
        if (currentUser.isSuperAdmin()) {
            base = pagoRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = pagoRepository.findByFacturaParqueaderoEmpresaId(empresaId);
        } else {
            base = pagoRepository.findByFacturaVehiculoPersonaId(currentUser.getCurrentPersonaId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagoDTO findById(Long id) {
        Pago p = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        Factura f = p.getFactura();
        if (currentUser.isSuperAdmin()) return toDTO(p);
        if (currentUser.isAdmin()) {
            if (f != null && f.getParqueadero() != null && f.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(f.getParqueadero().getEmpresa().getId());
            }
        } else {
            Long personaId = f != null && f.getVehiculo() != null && f.getVehiculo().getPersona() != null
                    ? f.getVehiculo().getPersona().getId() : null;
            currentUser.requireOwnerOrAnyAdmin(personaId);
        }
        return toDTO(p);
    }

    /**
     * Registra un pago. Blindado contra:
     * - RBAC: solo ADMIN/SUPER_ADMIN
     * - Multi-tenant: la factura debe ser de la empresa del operador
     * - Invariantes: monto > 0, monto <= saldo pendiente
     * - Inyeccion: el cliente NO setea fecha. Si la suma de pagos
     *   COMPLETADOs alcanza el valor total, factura pasa a PAGADA.
     */
    @Transactional
    public PagoDTO save(PagoDTO dto) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo el operador puede registrar pagos");
        }

        if (dto.getFacturaId() == null) {
            throw new BusinessException("facturaId es obligatorio", "ERR_MISSING_FIELDS");
        }
        Factura factura = facturaRepository.findById(dto.getFacturaId())
                .orElseThrow(() -> new ResourceNotFoundException("Factura", dto.getFacturaId()));

        // Multi-tenant
        if (factura.getParqueadero() != null && factura.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(factura.getParqueadero().getEmpresa().getId());
        }

        // Validar que la factura no este ya PAGADA o ANULADA
        if ("PAGADA".equals(factura.getEstado())) {
            throw new BusinessException("La factura ya esta pagada", "ERR_INVOICE_ALREADY_PAID");
        }
        if ("ANULADA".equals(factura.getEstado())) {
            throw new BusinessException("La factura esta anulada", "ERR_INVOICE_VOID");
        }

        // Invariantes de monto
        if (dto.getMonto() == null || dto.getMonto() <= 0) {
            throw new BusinessException(
                    "El monto debe ser mayor a cero",
                    "ERR_INVALID_AMOUNT");
        }

        double pagadoActual = pagoRepository.findByFacturaId(factura.getId()).stream()
                .filter(p -> "COMPLETADO".equals(p.getEstado()))
                .mapToDouble(Pago::getMonto)
                .sum();
        double saldoPendiente = factura.getValorTotal() - pagadoActual;

        if (dto.getMonto() > saldoPendiente + TOLERANCIA_DECIMAL) {
            throw new BusinessException(
                    "El monto " + dto.getMonto() + " excede el saldo pendiente " + saldoPendiente,
                    "ERR_INSUFFICIENT_FUNDS");
        }

        if (dto.getMetodo() == null || dto.getMetodo().isBlank()) {
            throw new BusinessException(
                    "metodo es obligatorio (EFECTIVO, TARJETA_CREDITO, TARJETA_DEBITO, NEQUI, DAVIPLATA, PSE, TRANSFERENCIA, FLYPASS, QR_BANCOLOMBIA, OTRO)",
                    "ERR_MISSING_FIELDS");
        }
        if (!MetodoPago.esValido(dto.getMetodo())) {
            throw new BusinessException(
                    "Metodo de pago no valido: " + dto.getMetodo(),
                    "ERR_INVALID_PAYMENT_METHOD");
        }

        // Sanitizacion: forzar fecha y estado por defecto
        Pago entity = new Pago();
        entity.setFactura(factura);
        entity.setMonto(dto.getMonto());
        entity.setMetodo(MetodoPago.normalizar(dto.getMetodo()));
        entity.setFechaHora(LocalDateTime.now()); // server time, ignorar dto
        // Estado por defecto COMPLETADO si no se especifica (registro manual del operador)
        // Si en el futuro se integra pasarela, deberia ser PENDIENTE hasta confirmar webhook.
        String estado = dto.getEstado() != null ? dto.getEstado() : "COMPLETADO";
        if (!estado.equals("COMPLETADO") && !estado.equals("PENDIENTE") && !estado.equals("FALLIDO")) {
            throw new BusinessException("Estado invalido: " + estado, "ERR_INVALID_STATE");
        }
        entity.setEstado(estado);

        Pago saved = pagoRepository.save(entity);

        // Si la suma de pagos COMPLETADOs cubre el valor total, marcar factura PAGADA
        if ("COMPLETADO".equals(estado)) {
            double totalPagado = pagadoActual + dto.getMonto();
            if (totalPagado >= factura.getValorTotal() - TOLERANCIA_DECIMAL) {
                factura.setEstado("PAGADA");
                facturaRepository.save(factura);
            }
        }

        return toDTO(saved);
    }

    /**
     * El update de un pago solo lo puede hacer SUPER_ADMIN
     * (caso reconciliacion / reverso). No se permite cambiar el monto
     * para evitar manipulaciones financieras.
     */
    @Transactional
    public PagoDTO update(Long id, PagoDTO dto) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede modificar un pago registrado");
        }
        Pago existing = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        // Solo permite cambiar estado (ej. marcar FALLIDO o COMPLETADO tras reconciliacion)
        if (dto.getEstado() != null) {
            existing.setEstado(dto.getEstado());
        }
        // monto, metodo y factura NO se pueden alterar
        return toDTO(pagoRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede eliminar pagos");
        }
        pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        pagoRepository.deleteById(id);
    }

    private PagoDTO toDTO(Pago e) {
        PagoDTO dto = new PagoDTO();
        dto.setId(e.getId());
        dto.setFechaHora(e.getFechaHora());
        dto.setMonto(e.getMonto());
        dto.setMetodo(e.getMetodo());
        dto.setEstado(e.getEstado());
        if (e.getFactura() != null) dto.setFacturaId(e.getFactura().getId());
        return dto;
    }
}
