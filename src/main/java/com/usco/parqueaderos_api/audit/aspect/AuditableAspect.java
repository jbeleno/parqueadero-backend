package com.usco.parqueaderos_api.audit.aspect;

import com.usco.parqueaderos_api.audit.context.AuditContextHolder;
import com.usco.parqueaderos_api.audit.service.AuditService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspecto que registra en audit_log toda invocacion a metodo anotado con @Auditable.
 *
 * Estrategia:
 *  - Para CREATE: solo "despues" (el resultado del metodo).
 *  - Para UPDATE / ARCHIVE / UNARCHIVE / ANULAR / DELETE_FISICO: no podemos saber
 *    "antes" sin reflection sobre el primer arg si es id (queda como mejora futura).
 *    Por ahora capturamos "despues" = resultado y los args como antes.
 *  - Para CALCULO / EXPORT / LOGIN: solo evento con metadata.
 *
 * Si Auditable.requiereMotivo() y no hay motivo en AuditContext -> rechaza la operacion
 * con ERR_MOTIVO_OBLIGATORIO.
 */
@Slf4j
@Aspect
@Component
@Order(50) // mas alto que @Transactional default para captar despues del commit logico
@RequiredArgsConstructor
public class AuditableAspect {

    private final AuditService auditService;

    @Around("@annotation(com.usco.parqueaderos_api.audit.aspect.Auditable)")
    public Object aroundAuditable(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((org.aspectj.lang.reflect.MethodSignature) pjp.getSignature()).getMethod();
        Auditable ann = method.getAnnotation(Auditable.class);

        if (ann.requiereMotivo()) {
            String motivo = AuditContextHolder.get() != null
                    ? AuditContextHolder.get().getMotivo() : null;
            if (motivo == null || motivo.trim().length() < 10) {
                throw new BusinessException(
                        "Esta operacion requiere motivo (header X-Motivo-Operacion, min 10 chars)",
                        "ERR_MOTIVO_OBLIGATORIO");
            }
        }

        Object[] args = pjp.getArgs();
        Object resultado;
        try {
            resultado = pjp.proceed();
        } catch (Throwable t) {
            // Registrar el intento fallido tambien
            try {
                auditService.log(ann.tabla(), null, ann.accion() + "_FALLIDO",
                        summary(args), null, null, "Excepcion: " + t.getClass().getSimpleName());
            } catch (Exception ignore) {}
            throw t;
        }

        try {
            Long id = extractId(resultado);
            Long empresaId = extractEmpresaId(resultado);
            auditService.log(ann.tabla(), id, ann.accion(),
                    "CREATE".equals(ann.accion()) ? null : summary(args),
                    resultado,
                    empresaId);
        } catch (Exception e) {
            log.warn("Audit log fallo para {}.{}: {}",
                    method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage());
        }
        return resultado;
    }

    private Object summary(Object[] args) {
        if (args == null || args.length == 0) return null;
        if (args.length == 1) return args[0];
        return java.util.Arrays.asList(args);
    }

    /** Best-effort: si el resultado tiene getId() lo extrae. Si no, null. */
    private Long extractId(Object o) {
        if (o == null) return null;
        try {
            Method m = o.getClass().getMethod("getId");
            Object v = m.invoke(o);
            return v instanceof Long ? (Long) v : null;
        } catch (Exception e) { return null; }
    }

    /** Best-effort: si el resultado tiene cadena getParqueadero().getEmpresa().getId(). */
    private Long extractEmpresaId(Object o) {
        if (o == null) return null;
        try {
            Method mp = o.getClass().getMethod("getParqueadero");
            Object p = mp.invoke(o);
            if (p == null) return null;
            Object e = p.getClass().getMethod("getEmpresa").invoke(p);
            if (e == null) return null;
            Object id = e.getClass().getMethod("getId").invoke(e);
            return id instanceof Long ? (Long) id : null;
        } catch (Exception ex) { return null; }
    }
}
