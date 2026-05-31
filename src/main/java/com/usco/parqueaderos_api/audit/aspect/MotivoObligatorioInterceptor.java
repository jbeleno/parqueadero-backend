package com.usco.parqueaderos_api.audit.aspect;

import com.usco.parqueaderos_api.audit.context.AuditContextFilter;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que exige el header X-Motivo-Operacion en TODA peticion no-GET
 * (POST/PUT/PATCH/DELETE) que toque endpoints de modificacion de datos.
 *
 * Excepciones razonables (no exige motivo):
 *  - /api/auth/* (login, refresh)
 *  - /api/health, /actuator/*
 *  - Endpoints de lectura (GET, HEAD, OPTIONS)
 *
 * El motivo debe tener minimo 10 caracteres.
 */
@Component
@RequiredArgsConstructor
public class MotivoObligatorioInterceptor implements HandlerInterceptor {

    private static final int MIN_LEN = 10;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String method = req.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) return true;

        String uri = req.getRequestURI();
        if (uri.startsWith("/api/auth/")
                || uri.startsWith("/api/health")
                || uri.startsWith("/actuator")
                || uri.startsWith("/ws/")
                || uri.equals("/error")
                // Sidecar OCR y flujos automaticos no exigen motivo humano
                || uri.startsWith("/api/ocr/")
                || uri.matches("^/api/camaras/\\d+/imagen$")
                || uri.equals("/api/tickets/auto")
                // Registro de cliente USER (autoservicio)
                || uri.startsWith("/api/personas")
                || uri.startsWith("/api/vehiculos") && "POST".equals(method)
                || uri.equals("/api/usuarios")
                // v49: ejecutar reportes es READ-ONLY (usa POST por tamaño del JSON de
                // filtros, no porque mute datos). El service ya audita la ejecucion
                // en reporte_ejecutado con duracion, error, etc.
                || uri.matches("^/api/reportes-parametrizables/[^/]+/ejecutar$")) {
            return true;
        }

        String motivo = req.getHeader(AuditContextFilter.HEADER_MOTIVO);
        if (motivo != null && motivo.trim().length() >= MIN_LEN) return true;

        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setContentType("application/json");
        res.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(
                        "Header X-Motivo-Operacion es obligatorio (min " + MIN_LEN + " caracteres) " +
                        "para toda operacion modificadora",
                        "ERR_MOTIVO_OBLIGATORIO")));
        return false;
    }
}
