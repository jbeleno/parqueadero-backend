package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.audit.service.AuditService;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.ConfiguracionReciboDTO;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Lectura y edicion auditada de la configuracion de RECIBO de un parqueadero.
 *
 * Reglas:
 *  - GET: cualquier rol con acceso al parqueadero.
 *  - PATCH: ADMIN (empresa), ADMIN_PARQUEADERO (parq asignado), SUPER_ADMIN.
 *  - Motivo obligatorio (>=10 chars) en PATCH — capturado por interceptor global,
 *    pero ademas auditamos snapshot antes/despues con detalle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracionReciboService {

    private final ParqueaderoRepository parqueaderoRepository;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public ConfiguracionReciboDTO obtener(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        return toDTO(p);
    }

    @Transactional
    public ConfiguracionReciboDTO actualizar(Long parqueaderoId, ConfiguracionReciboDTO dto) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));

        // RBAC
        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                if (p.getEmpresa() != null) currentUser.requireEmpresa(p.getEmpresa().getId());
            } else if (currentUser.isAdminParqueadero()) {
                if (!currentUser.getParqueaderoIds().contains(parqueaderoId)) {
                    throw new AccessDeniedException("Parqueadero no asignado");
                }
            } else {
                throw new AccessDeniedException("Sin permisos para editar configuracion de recibo");
            }
        }

        // Snapshot antes
        Map<String, Object> antes = snapshot(p);

        if (dto.getResolucionDian() != null) p.setResolucionDian(dto.getResolucionDian());
        if (dto.getPieRecibo() != null) p.setPieRecibo(dto.getPieRecibo());
        if (dto.getEncabezadoRecibo() != null) p.setEncabezadoRecibo(dto.getEncabezadoRecibo());
        if (dto.getRegimenTributario() != null) p.setRegimenTributario(dto.getRegimenTributario());
        if (dto.getLogoUrl() != null) p.setLogoUrl(dto.getLogoUrl());

        Parqueadero saved = parqueaderoRepository.save(p);
        Map<String, Object> despues = snapshot(saved);

        audit.log("parqueadero", parqueaderoId, "UPDATE_RECIBO_CONFIG", antes, despues,
                p.getEmpresa() != null ? p.getEmpresa().getId() : null);

        log.info("ConfiguracionRecibo parq={} actualizada por usr={}",
                parqueaderoId, currentUser.getCurrentUserId());
        return toDTO(saved);
    }

    private Map<String, Object> snapshot(Parqueadero p) {
        Map<String, Object> m = new HashMap<>();
        m.put("resolucionDian", p.getResolucionDian());
        m.put("pieRecibo", p.getPieRecibo());
        m.put("encabezadoRecibo", p.getEncabezadoRecibo());
        m.put("regimenTributario", p.getRegimenTributario());
        m.put("logoUrl", p.getLogoUrl());
        return m;
    }

    private ConfiguracionReciboDTO toDTO(Parqueadero p) {
        return new ConfiguracionReciboDTO(
                p.getResolucionDian(),
                p.getPieRecibo(),
                p.getEncabezadoRecibo(),
                p.getRegimenTributario(),
                p.getLogoUrl()
        );
    }
}
