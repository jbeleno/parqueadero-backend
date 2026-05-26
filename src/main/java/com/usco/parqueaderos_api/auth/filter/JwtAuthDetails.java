package com.usco.parqueaderos_api.auth.filter;

import java.util.List;

/**
 * Detalles extra del Authentication leidos del JWT.
 * - empresaId: para ADMIN (acceso global a su empresa) o denormalizacion.
 * - parqueaderoIds: para OPERARIO_CAJA / ADMIN_PARQUEADERO (asignacion granular).
 */
public record JwtAuthDetails(Long empresaId, List<Long> parqueaderoIds) {
    public JwtAuthDetails(Long empresaId) { this(empresaId, List.of()); }
}
