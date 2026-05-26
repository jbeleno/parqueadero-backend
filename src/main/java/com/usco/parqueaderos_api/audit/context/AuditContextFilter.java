package com.usco.parqueaderos_api.audit.context;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP que prepara el AuditContext para cada request.
 * Captura: usuario actual, IP, user-agent, request_id, endpoint, motivo (header).
 *
 * Limpia el ThreadLocal en finally para evitar leaks entre requests.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
public class AuditContextFilter extends OncePerRequestFilter {

    public static final String HEADER_MOTIVO = "X-Motivo-Operacion";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final CurrentUserService currentUser;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        AuditContext ctx = AuditContext.builder()
                .usuarioId(safeUserId())
                .origen("HTTP")
                .motivo(req.getHeader(HEADER_MOTIVO))
                .requestId(req.getHeader(HEADER_REQUEST_ID) != null
                        ? req.getHeader(HEADER_REQUEST_ID)
                        : UUID.randomUUID().toString())
                .endpoint(req.getMethod() + " " + req.getRequestURI())
                .ip(extractIp(req))
                .userAgent(req.getHeader("User-Agent"))
                .build();
        AuditContextHolder.set(ctx);
        // Exponer el request_id en la respuesta para correlacion con el front
        res.setHeader(HEADER_REQUEST_ID, ctx.getRequestId());
        try {
            chain.doFilter(req, res);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private Long safeUserId() {
        try {
            return currentUser.getCurrentUserId();
        } catch (Exception e) {
            return null; // no autenticado o pre-auth
        }
    }

    private String extractIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
