package com.usco.parqueaderos_api.audit.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de service como auditable. El aspecto AuditableAspect
 * intercepta y registra en audit_log con valores antes/despues si aplica.
 *
 * Ejemplos:
 *   @Auditable(tabla = "factura", accion = "ANULAR")
 *   @Auditable(tabla = "tarifa", accion = "CREATE")
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    /** Tabla afectada. Obligatorio. */
    String tabla();
    /** Accion: CREATE | UPDATE | ARCHIVE | UNARCHIVE | ANULAR | DELETE_FISICO | EXPORT | CALCULO */
    String accion();
    /** Si true, motivo es obligatorio en el AuditContext. Default true para destructivas. */
    boolean requiereMotivo() default false;
}
