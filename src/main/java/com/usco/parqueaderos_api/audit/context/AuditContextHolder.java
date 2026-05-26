package com.usco.parqueaderos_api.audit.context;

import java.util.function.Supplier;

/**
 * ThreadLocal storage del AuditContext. Cada request HTTP / job tiene su propio contexto.
 * IMPORTANTE: el filtro HTTP debe llamar clear() en finally para evitar leaks entre requests.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> HOLDER = new ThreadLocal<>();

    private AuditContextHolder() {}

    public static void set(AuditContext ctx) {
        HOLDER.set(ctx);
    }

    public static AuditContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** Ejecuta un bloque con un AuditContext especifico (jobs y listeners). */
    public static <T> T runAs(AuditContext ctx, Supplier<T> block) {
        AuditContext prev = HOLDER.get();
        try {
            HOLDER.set(ctx);
            return block.get();
        } finally {
            if (prev != null) HOLDER.set(prev);
            else HOLDER.remove();
        }
    }

    public static void runAs(AuditContext ctx, Runnable block) {
        runAs(ctx, () -> { block.run(); return null; });
    }
}
