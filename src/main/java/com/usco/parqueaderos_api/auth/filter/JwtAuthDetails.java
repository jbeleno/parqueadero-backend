package com.usco.parqueaderos_api.auth.filter;

/**
 * Detalles extra del Authentication para llevar el empresaId
 * extraido del JWT y evitar consultas a BD en cada request.
 */
public record JwtAuthDetails(Long empresaId) {
}
