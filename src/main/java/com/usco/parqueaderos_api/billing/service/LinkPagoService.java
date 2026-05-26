package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Stub local de generacion de link de pago. NO integra con pasarela real
 * todavia (Wompi/PSE/Mercado Pago). Genera una referencia y URL placeholder
 * para que el front pueda implementar el flujo end-to-end mientras se
 * decide el proveedor.
 *
 * El webhook real (cuando se integre) creara un Pago COMPLETADO via
 * PagoService.save y se acoplara al flujo existente de
 * "factura PAGADA cuando suma cubre el total".
 */
@Service
@RequiredArgsConstructor
public class LinkPagoService {

    private final FacturaRepository facturaRepository;
    private final CurrentUserService currentUser;

    @Value("${app.pago.base-url:https://stub.pagos.local/checkout}")
    private String baseUrlStub;

    @Transactional(readOnly = true)
    public LinkPagoResponse generar(Long facturaId, Double montoSolicitado) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", facturaId));
        if ("PAGADA".equals(f.getEstado()) || "ANULADA".equals(f.getEstado())) {
            throw new BusinessException(
                    "La factura no requiere pago (estado=" + f.getEstado() + ")",
                    "ERR_INVOICE_NOT_PAYABLE");
        }
        if (montoSolicitado == null || montoSolicitado <= 0) {
            throw new BusinessException("monto > 0 obligatorio", "ERR_INVALID_AMOUNT");
        }
        if (montoSolicitado > f.getValorTotal() + 0.01) {
            throw new BusinessException(
                    "monto " + montoSolicitado + " excede el valor de la factura " + f.getValorTotal(),
                    "ERR_INVALID_AMOUNT");
        }
        // RBAC: USER pago de sus propias facturas; ADMIN cualquiera de su empresa
        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                if (f.getParqueadero() != null && f.getParqueadero().getEmpresa() != null) {
                    currentUser.requireEmpresa(f.getParqueadero().getEmpresa().getId());
                }
            } else {
                Long personaId = f.getVehiculo() != null && f.getVehiculo().getPersona() != null
                        ? f.getVehiculo().getPersona().getId() : null;
                currentUser.requireOwnerOrAnyAdmin(personaId);
            }
        }
        String referencia = "PARQ-" + facturaId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String url = baseUrlStub + "?ref=" + referencia + "&monto=" + montoSolicitado;
        return new LinkPagoResponse(referencia, url, "STUB", "Integrar proveedor real (Wompi/PSE) en webhook");
    }

    @Data
    public static class LinkPagoRequest {
        private Long facturaId;
        private Double monto;
    }

    public record LinkPagoResponse(String referencia, String urlPago, String proveedor, String nota) {}
}
