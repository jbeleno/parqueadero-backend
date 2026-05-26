package com.usco.parqueaderos_api.report.universal.spec;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Catalogo de specs de reportes universales. Cada @Component es registrado
 * por Spring y descubierto por ReporteUniversalService.
 *
 * Cada SQL usa placeholders opcionales :parqueaderoId :empresaId :desde :hasta.
 *
 * Roles permitidos: por consistencia con RBAC general.
 */
public class ReportesSpecs {

    // ─── TICKETS ─────────────────────────────────────────────────────────
    @Component
    public static class Tickets extends BaseSqlReporteSpec {
        public String clave() { return "tickets"; }
        public String titulo() { return "Tickets"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO", "OPERARIO_CAJA");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Ticket");
            c.put("placa", "Placa");
            c.put("parqueadero", "Parqueadero");
            c.put("entrada", "Entrada");
            c.put("salida", "Salida");
            c.put("duracion_min", "Duracion (min)");
            c.put("estado", "Estado");
            c.put("monto", "Monto");
            c.put("suscripcion_id", "Suscripcion");
            return c;
        }
        protected String sqlBase() {
            return "SELECT t.id, v.placa, p.nombre," +
                   "       t.fecha_hora_entrada, t.fecha_hora_salida," +
                   "       CASE WHEN t.fecha_hora_salida IS NULL THEN NULL " +
                   "            ELSE EXTRACT(EPOCH FROM (t.fecha_hora_salida - t.fecha_hora_entrada))/60 END," +
                   "       t.estado, t.monto_calculado, t.suscripcion_id " +
                   "  FROM ticket t " +
                   "  JOIN vehiculo v ON v.id = t.vehiculo_id " +
                   "  JOIN parqueadero p ON p.id = t.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR t.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId    IS NULL OR p.empresa_id      = :empresaId)" +
                   "   AND (:desde IS NULL OR t.fecha_hora_entrada >= :desde)" +
                   "   AND (:hasta IS NULL OR t.fecha_hora_entrada <= :hasta)" +
                   " ORDER BY t.fecha_hora_entrada DESC";
        }
    }

    // ─── PAGOS ───────────────────────────────────────────────────────────
    @Component
    public static class Pagos extends BaseSqlReporteSpec {
        public String clave() { return "pagos"; }
        public String titulo() { return "Pagos"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO", "OPERARIO_CAJA");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Pago");
            c.put("factura_id", "Factura");
            c.put("fecha", "Fecha");
            c.put("metodo", "Metodo");
            c.put("monto", "Monto");
            c.put("estado", "Estado");
            c.put("motivo_anulacion", "Motivo anulacion");
            return c;
        }
        protected String sqlBase() {
            return "SELECT pa.id, pa.factura_id, pa.fecha_hora, pa.metodo, pa.monto, pa.estado, pa.motivo_anulacion " +
                   "  FROM pago pa JOIN factura f ON f.id = pa.factura_id " +
                   " WHERE (:parqueaderoId IS NULL OR f.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR f.parqueadero_id IN " +
                   "        (SELECT id FROM parqueadero WHERE empresa_id = :empresaId))" +
                   "   AND (:desde IS NULL OR pa.fecha_hora >= :desde)" +
                   "   AND (:hasta IS NULL OR pa.fecha_hora <= :hasta)" +
                   " ORDER BY pa.fecha_hora DESC";
        }
    }

    // ─── FACTURAS ────────────────────────────────────────────────────────
    @Component
    public static class Facturas extends BaseSqlReporteSpec {
        public String clave() { return "facturas"; }
        public String titulo() { return "Facturas"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Factura");
            c.put("ticket_id", "Ticket");
            c.put("placa", "Placa");
            c.put("fecha", "Fecha");
            c.put("base", "Base imponible");
            c.put("iva", "IVA");
            c.put("total", "Total");
            c.put("estado", "Estado");
            c.put("origen", "Origen");
            return c;
        }
        protected String sqlBase() {
            return "SELECT f.id, f.ticket_id, v.placa, f.fecha_hora, " +
                   "       f.base_imponible, f.iva_monto, f.valor_total, f.estado, f.origen " +
                   "  FROM factura f JOIN vehiculo v ON v.id = f.vehiculo_id " +
                   " WHERE (:parqueaderoId IS NULL OR f.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR f.parqueadero_id IN " +
                   "        (SELECT id FROM parqueadero WHERE empresa_id = :empresaId))" +
                   "   AND (:desde IS NULL OR f.fecha_hora >= :desde)" +
                   "   AND (:hasta IS NULL OR f.fecha_hora <= :hasta)" +
                   " ORDER BY f.fecha_hora DESC";
        }
    }

    // ─── CAJAS ────────────────────────────────────────────────────────────
    @Component
    public static class Cajas extends BaseSqlReporteSpec {
        public String clave() { return "cajas"; }
        public String titulo() { return "Cajas y arqueos"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO", "OPERARIO_CAJA");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Caja");
            c.put("parqueadero", "Parqueadero");
            c.put("operador", "Operador");
            c.put("nombre", "Nombre");
            c.put("fondo_inicial", "Fondo inicial");
            c.put("calculado", "Saldo calculado");
            c.put("contado", "Saldo contado");
            c.put("diferencia", "Diferencia");
            c.put("estado", "Estado");
            c.put("abierta_en", "Abierta");
            c.put("cerrada_en", "Cerrada");
            return c;
        }
        protected String sqlBase() {
            return "SELECT c.id, p.nombre, u.correo, c.nombre, c.fondo_inicial, c.saldo_calculado, " +
                   "       c.saldo_contado, c.diferencia, c.estado, c.abierta_en, c.cerrada_en " +
                   "  FROM caja c " +
                   "  JOIN parqueadero p ON p.id = c.parqueadero_id " +
                   "  JOIN usuario u ON u.id = c.usuario_id " +
                   " WHERE (:parqueaderoId IS NULL OR c.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR c.abierta_en >= :desde)" +
                   "   AND (:hasta IS NULL OR c.abierta_en <= :hasta)" +
                   " ORDER BY c.abierta_en DESC";
        }
    }

    // ─── MOVIMIENTOS CAJA ────────────────────────────────────────────────
    @Component
    public static class MovimientosCaja extends BaseSqlReporteSpec {
        public String clave() { return "movimientos-caja"; }
        public String titulo() { return "Movimientos de caja"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO", "OPERARIO_CAJA");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Movimiento");
            c.put("caja_id", "Caja");
            c.put("tipo", "Tipo");
            c.put("monto", "Monto");
            c.put("saldo", "Saldo");
            c.put("pago_id", "Pago");
            c.put("motivo", "Motivo");
            c.put("operador", "Operador");
            c.put("fecha", "Fecha");
            return c;
        }
        protected String sqlBase() {
            return "SELECT m.id, m.caja_id, m.tipo, m.monto, m.saldo_resultante, m.pago_id, " +
                   "       m.motivo, u.correo, m.fecha_hora " +
                   "  FROM movimiento_caja m " +
                   "  JOIN caja c ON c.id = m.caja_id " +
                   "  JOIN usuario u ON u.id = m.usuario_id " +
                   "  JOIN parqueadero p ON p.id = c.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR c.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR m.fecha_hora >= :desde)" +
                   "   AND (:hasta IS NULL OR m.fecha_hora <= :hasta)" +
                   " ORDER BY m.fecha_hora DESC";
        }
    }

    // ─── VEHICULOS ───────────────────────────────────────────────────────
    @Component
    public static class Vehiculos extends BaseSqlReporteSpec {
        public String clave() { return "vehiculos"; }
        public String titulo() { return "Vehiculos"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("placa", "Placa");
            c.put("tipo", "Tipo");
            c.put("color", "Color");
            c.put("persona", "Persona");
            c.put("documento", "Documento");
            c.put("activo", "Activo");
            c.put("es_visitante", "Visitante");
            c.put("ultima_actividad", "Ultima actividad");
            return c;
        }
        protected String sqlBase() {
            return "SELECT v.id, v.placa, tv.nombre, v.color, " +
                   "       COALESCE(p.nombre || ' ' || p.apellido, ''), p.numero_documento, " +
                   "       v.activo, v.es_visitante, v.ultima_actividad " +
                   "  FROM vehiculo v " +
                   "  LEFT JOIN tipo_vehiculo tv ON tv.id = v.tipo_vehiculo_id " +
                   "  LEFT JOIN persona p ON p.id = v.persona_id " +
                   " ORDER BY v.id DESC";
        }
    }

    // ─── SUSCRIPCIONES ───────────────────────────────────────────────────
    @Component
    public static class Suscripciones extends BaseSqlReporteSpec {
        public String clave() { return "suscripciones"; }
        public String titulo() { return "Suscripciones"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO", "OPERARIO_CAJA");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "Suscripcion");
            c.put("placa", "Placa");
            c.put("parqueadero", "Parqueadero");
            c.put("tipo", "Tipo");
            c.put("estado", "Estado");
            c.put("inicio", "Inicio");
            c.put("fin", "Fin");
            c.put("monto_pagado", "Monto pagado");
            c.put("saldo", "Saldo");
            c.put("punto_reservado", "Punto reservado");
            return c;
        }
        protected String sqlBase() {
            return "SELECT s.id, v.placa, p.nombre, s.tipo, s.estado, " +
                   "       s.fecha_inicio, s.fecha_fin, s.monto_pagado, s.saldo_restante, " +
                   "       s.punto_parqueo_reservado_id " +
                   "  FROM suscripcion s " +
                   "  JOIN vehiculo v ON v.id = s.vehiculo_id " +
                   "  JOIN parqueadero p ON p.id = s.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR s.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR s.fecha_inicio >= :desde)" +
                   "   AND (:hasta IS NULL OR s.fecha_inicio <= :hasta)" +
                   " ORDER BY s.fecha_inicio DESC";
        }
    }

    // ─── MOVIMIENTOS SALDO ───────────────────────────────────────────────
    @Component
    public static class MovimientosSaldo extends BaseSqlReporteSpec {
        public String clave() { return "movimientos-saldo"; }
        public String titulo() { return "Movimientos de saldo prepago"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("suscripcion_id", "Suscripcion");
            c.put("tipo", "Tipo");
            c.put("monto", "Monto");
            c.put("saldo", "Saldo resultante");
            c.put("ticket_id", "Ticket");
            c.put("motivo", "Motivo");
            c.put("fecha", "Fecha");
            return c;
        }
        protected String sqlBase() {
            return "SELECT m.id, m.suscripcion_id, m.tipo, m.monto, m.saldo_resultante, " +
                   "       m.ticket_id, m.motivo, m.fecha " +
                   "  FROM movimiento_saldo m " +
                   "  JOIN suscripcion s ON s.id = m.suscripcion_id " +
                   "  JOIN parqueadero p ON p.id = s.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR s.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR m.fecha >= :desde)" +
                   "   AND (:hasta IS NULL OR m.fecha <= :hasta)" +
                   " ORDER BY m.fecha DESC";
        }
    }

    // ─── TARIFAS ─────────────────────────────────────────────────────────
    @Component
    public static class Tarifas extends BaseSqlReporteSpec {
        public String clave() { return "tarifas"; }
        public String titulo() { return "Tarifas"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("nombre", "Nombre");
            c.put("parqueadero", "Parqueadero");
            c.put("tipo_vehiculo", "Tipo");
            c.put("valor", "Valor");
            c.put("unidad", "Unidad");
            c.put("gracia", "Gracia (min)");
            c.put("minimo", "Minimo");
            c.put("cubre", "Cubre (min)");
            c.put("aplica_iva", "Aplica IVA");
            c.put("iva_pct", "IVA %");
            c.put("activo", "Activa");
            return c;
        }
        protected String sqlBase() {
            return "SELECT t.id, t.nombre, p.nombre, tv.nombre, t.valor, t.unidad, " +
                   "       t.minutos_gracia, t.valor_minimo, t.minutos_cubiertos_por_minimo, " +
                   "       t.aplica_iva, t.iva_porcentaje, t.activo " +
                   "  FROM tarifa t " +
                   "  JOIN parqueadero p ON p.id = t.parqueadero_id " +
                   "  LEFT JOIN tipo_vehiculo tv ON tv.id = t.tipo_vehiculo_id " +
                   " WHERE (:parqueaderoId IS NULL OR t.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   " ORDER BY t.id";
        }
    }

    // ─── CIERRES DIA ─────────────────────────────────────────────────────
    @Component
    public static class CierresDia extends BaseSqlReporteSpec {
        public String clave() { return "cierres-dia"; }
        public String titulo() { return "Cierres diarios"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("parqueadero", "Parqueadero");
            c.put("fecha", "Fecha");
            c.put("tickets_cerrados", "Tickets");
            c.put("total_cobrado", "Cobrado");
            c.put("efectivo", "Efectivo");
            c.put("tarjeta", "Tarjeta");
            c.put("otros", "Otros");
            c.put("facturas", "Facturas");
            c.put("pendiente", "Pendiente");
            c.put("anulados", "Anulados");
            return c;
        }
        protected String sqlBase() {
            return "SELECT c.id, p.nombre, c.fecha, c.tickets_cerrados, c.total_cobrado, " +
                   "       c.total_efectivo, c.total_tarjeta, c.total_otros, " +
                   "       c.facturas_emitidas, c.total_pendiente, c.tickets_anulados " +
                   "  FROM cierre_dia c " +
                   "  JOIN parqueadero p ON p.id = c.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR c.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR c.fecha >= :desde)" +
                   "   AND (:hasta IS NULL OR c.fecha <= :hasta)" +
                   " ORDER BY c.fecha DESC";
        }
    }

    // ─── CONVENIOS ───────────────────────────────────────────────────────
    @Component
    public static class Convenios extends BaseSqlReporteSpec {
        public String clave() { return "convenios"; }
        public String titulo() { return "Convenios y descuentos"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("comercio", "Comercio");
            c.put("nit", "NIT");
            c.put("parqueadero", "Parqueadero");
            c.put("tipo", "Tipo descuento");
            c.put("valor_fijo", "Valor fijo");
            c.put("porcentaje", "Porcentaje");
            c.put("minutos_gratis", "Min. gratis");
            c.put("monto_minimo", "Monto minimo compra");
            c.put("vigencia_fin", "Vigencia fin");
            c.put("activo", "Activo");
            return c;
        }
        protected String sqlBase() {
            return "SELECT c.id, c.nombre_comercio, c.nit_comercio, p.nombre, c.tipo_descuento, " +
                   "       c.valor_descuento, c.porcentaje_descuento, c.minutos_gratis, " +
                   "       c.monto_minimo_compra, c.fecha_fin_vigencia, c.activo " +
                   "  FROM convenio c " +
                   "  JOIN parqueadero p ON p.id = c.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR c.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   " ORDER BY c.id DESC";
        }
    }

    // ─── ASIGNACIONES ────────────────────────────────────────────────────
    @Component
    public static class Asignaciones extends BaseSqlReporteSpec {
        public String clave() { return "asignaciones"; }
        public String titulo() { return "Asignaciones usuario-parqueadero"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("usuario", "Usuario");
            c.put("parqueadero", "Parqueadero");
            c.put("rol", "Rol");
            c.put("activo", "Activo");
            c.put("asignado_en", "Asignado");
            c.put("desasignado_en", "Desasignado");
            c.put("motivo", "Motivo desasignacion");
            return c;
        }
        protected String sqlBase() {
            return "SELECT u.correo, p.nombre, r.nombre, up.activo, " +
                   "       up.asignado_en, up.desasignado_en, up.motivo_desasignacion " +
                   "  FROM usuario_parqueadero up " +
                   "  JOIN usuario u ON u.id = up.usuario_id " +
                   "  JOIN parqueadero p ON p.id = up.parqueadero_id " +
                   "  JOIN rol r ON r.id = up.rol_id " +
                   " WHERE (:parqueaderoId IS NULL OR up.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   " ORDER BY up.asignado_en DESC";
        }
    }

    // ─── AUDIT LOG ───────────────────────────────────────────────────────
    @Component
    public static class AuditLogReport extends BaseSqlReporteSpec {
        public String clave() { return "audit-log"; }
        public String titulo() { return "Auditoria"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("tabla", "Tabla");
            c.put("registro_id", "Registro");
            c.put("accion", "Accion");
            c.put("usuario_id", "Usuario");
            c.put("motivo", "Motivo");
            c.put("endpoint", "Endpoint");
            c.put("ip", "IP");
            c.put("fecha", "Fecha");
            return c;
        }
        protected String sqlBase() {
            return "SELECT id, tabla, registro_id, accion, usuario_id, motivo, endpoint, ip, fecha_hora " +
                   "  FROM audit_log " +
                   " WHERE (:empresaId IS NULL OR empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR fecha_hora >= :desde)" +
                   "   AND (:hasta IS NULL OR fecha_hora <= :hasta)" +
                   " ORDER BY fecha_hora DESC";
        }
    }

    // ─── VALIDACIONES COMPRA ─────────────────────────────────────────────
    @Component
    public static class ValidacionesCompra extends BaseSqlReporteSpec {
        public String clave() { return "validaciones-compra"; }
        public String titulo() { return "Validaciones de compra (convenios)"; }
        public Set<String> rolesPermitidos() {
            return Set.of("ADMIN", "SUPER_ADMIN", "ADMIN_PARQUEADERO");
        }
        public Map<String, String> columnas() {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", "ID");
            c.put("ticket_id", "Ticket");
            c.put("convenio", "Convenio");
            c.put("monto_compra", "Monto compra");
            c.put("folio", "Folio");
            c.put("descuento", "Descuento");
            c.put("fecha", "Fecha");
            return c;
        }
        protected String sqlBase() {
            return "SELECT vc.id, vc.ticket_id, c.nombre_comercio, vc.monto_compra, " +
                   "       vc.folio_externo, vc.descuento_aplicado, vc.fecha_aplicacion " +
                   "  FROM validacion_compra vc " +
                   "  JOIN convenio c ON c.id = vc.convenio_id " +
                   "  JOIN parqueadero p ON p.id = c.parqueadero_id " +
                   " WHERE (:parqueaderoId IS NULL OR c.parqueadero_id = :parqueaderoId)" +
                   "   AND (:empresaId IS NULL OR p.empresa_id = :empresaId)" +
                   "   AND (:desde IS NULL OR vc.fecha_aplicacion >= :desde)" +
                   "   AND (:hasta IS NULL OR vc.fecha_aplicacion <= :hasta)" +
                   " ORDER BY vc.fecha_aplicacion DESC";
        }
    }

    private ReportesSpecs() {}
}
