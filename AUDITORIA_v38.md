# Auditoría v38 — Realismo, schemas, flujos, integridad

Fecha: 2026-05-26
Versión auditada: post-commits `41931fa` → `3a5bf96` (v37 → v38)
Tests: 115 verde

---

## 1. Hallazgos críticos arreglados en este commit

| # | Hallazgo | Impacto | Estado |
|---|---|---|---|
| H1 | `MovimientoSaldo` schema en `data.sql` divergía de la entidad (columnas fantasma `tipo`, `saldo_antes/despues`, `descripcion`, `fecha_hora`; faltaba `pago_id`) | Si Hibernate `ddl-auto=none` en algún ambiente, la app no arrancaba. En prod no rompió porque Hibernate `update` ganó primero | **Arreglado** — `CREATE TABLE` alineado a la entidad |
| H2 | `TicketDTO` no exponía `suscripcionId` ni `fechaHoraSalidaFisica` | El front nunca veía qué suscripción cubrió el ticket ni cuándo cruzó físicamente la salida — features documentadas pero invisibles | **Arreglado** — TicketDTO y TicketService.toDTO actualizados |
| H3 | `CobroResult.facturaPendiente=true` no creaba factura. La factura solo se creaba con `POST /api/facturas` manual | El operador tenía que recordar crear cada factura tras cerrar ticket. En la práctica = facturas perdidas | **Arreglado** — `AutoFacturaListener` crea factura PENDIENTE automáticamente al cerrar ticket (idempotente, REQUIRES_NEW) |
| H4 | `Tarifa.aplicaIva` e `ivaPorcentaje` existían pero nunca se persistían en la `Factura`. `TarifaCalculatorService.desagregarIva` no se llamaba | Configurar IVA en una tarifa no servía para nada — no aparecía en la factura | **Arreglado** — `Factura` con `baseImponible` + `ivaMonto` + `ivaPorcentaje`. Listener y `FacturaService.save` desagregan |

---

## 2. Gaps de realismo identificados — pendientes (no implementados)

Estos son aspectos del negocio "parqueadero real colombiano" que tu sistema **no cubre todavía**. Los listo con severidad para que decidas qué priorizar.

### 2.1 Severidad ALTA (operación real ya los necesita)

**G1 — Anulación de pago / reverso**
- Hoy `PagoService.update` solo permite cambiar estado (SUPER_ADMIN). No hay flujo de "anular pago por reverso de banco / chargeback".
- Realista: el cliente pagó con tarjeta, el banco hace reverso al día siguiente, la factura debe volver a PENDIENTE.
- **Solución sugerida**: endpoint `PATCH /api/pagos/{id}/anular?motivo=...` que cambie estado a FALLIDO y recalcule estado de la factura. Lo emite SUPER_ADMIN.

**G2 — Cancelación de ticket EN_CURSO**
- Solo se puede `ANULAR` un ticket EN_CURSO. No hay "ticket abandonado/extraviado por el operador" con motivo.
- Realista: cliente perdió el ticket físico → operador necesita registrar entrada+salida pero sin ticket original. Hoy se anula y se crea uno nuevo manual, sin trazabilidad.
- **Solución sugerida**: campo `motivoAnulacion` + `anuladoPor` en Ticket. Endpoint dedicado `PATCH /api/tickets/{id}/anular` con justificación obligatoria.

**G3 — Estado del vehículo (soft-delete)**
- `Vehiculo` no tiene `estado_id` ni `activo`. Cuando un cliente vende su carro, no hay forma de "archivar" — solo eliminar (que ahora bloquea con 409 si hay historial).
- Realista: el cliente vende su auto, el operador quiere marcar el vehículo como "inactivo" para que no aparezca en sugerencias del front pero conservar el historial.
- **Solución sugerida**: `Vehiculo.activo BOOLEAN DEFAULT true` + `PATCH /api/vehiculos/{id}/archivar`. Filtrar `activo=true` en `findAll`.

**G4 — Tipo de movimiento en `MovimientoSaldo`**
- Hoy `motivo` es texto libre. No hay enum para distinguir ABONO/CONSUMO/REVERSO/AJUSTE_ADMIN.
- Realista: reportes financieros necesitan diferenciar carga del cliente vs ajuste manual del operador.
- **Solución sugerida**: añadir `tipo` enum (`ABONO, CONSUMO, REVERSO, AJUSTE`) en la entidad + columna.

### 2.2 Severidad MEDIA (mejora la experiencia pero no rompe operación)

**G5 — Cliente sin cuenta (visitante / "guest")**
- Hoy todo `Ticket` requiere `Vehiculo`. Si llega un visitante esporádico, el operador crea un vehículo "fantasma" sin persona.
- Realista: parqueaderos públicos atienden 80% visitantes únicos. Marcar esos vehículos como "guest" simplifica reportes.
- **Solución sugerida**: `Vehiculo.esVisitante BOOLEAN` + opción de auto-archivado tras X días sin actividad.

**G6 — Recibo del ticket cerrado (PDF/imagen)**
- No hay endpoint para generar el recibo. El front lo arma con los datos del DTO.
- Realista: parqueaderos formales (DIAN) deben emitir factura electrónica con CUFE; informales emiten recibo en papel térmico.
- **Solución sugerida (futura)**: `GET /api/facturas/{id}/recibo.pdf`. Lo dejé fuera de scope por explícito pedido del usuario ("todo menos DIAN").

**G7 — Reservas + suscripción**
- Una reserva con horario "siempre" (mensualidad de espacio fijo) no existe. La `Suscripcion` MENSUAL da derecho a entrar/salir, pero NO reserva un punto específico.
- Realista: oficinas pagan mensualidad por un puesto FIJO (oficinas, colegios, conjuntos residenciales).
- **Solución sugerida**: agregar `puntoParqueoIdReservado` a `Suscripcion`. Si está presente, ese punto queda bloqueado para uso externo durante la vigencia.

**G8 — Notificación al cliente (push / email / SMS)**
- Las notificaciones WebSocket llegan al **operador** (topic del parqueadero). El cliente USER no recibe nada en su app.
- Realista: el USER necesita saber "tu mensualidad vence en 3 días" o "tu saldo se acabó".
- **Solución sugerida**: `notificarUsuario(usuarioId)` ya existe en NotificationService pero no se usa para vencimientos/saldo bajo. Conectar.

**G9 — Pagos parciales en línea (link de pago)**
- Hoy todos los pagos se registran "manualmente" por el operador. No hay integración con PSE/Wompi/Mercado Pago.
- Realista: el cliente quiere pagar su deuda desde la app.
- **Solución sugerida**: endpoint `POST /api/pagos/link-pago` que genere link de pasarela. Stub interno por ahora, integración real después.

### 2.3 Severidad BAJA (nice-to-have, optimizaciones)

**G10 — Métricas operacionales (no facturación)**
- Tiempos promedio de estadía, horas pico, ocupación máxima diaria — todo eso falta. Solo hay métricas de plata.
- **Solución**: 1 endpoint `/api/reportes/operacion` con percentiles de duración, ocupación horaria.

**G11 — Auditoría de cambios en tarifa**
- Si un admin cambia el `valor` de una tarifa mientras hay tickets EN_CURSO, esos tickets verán el valor nuevo al cerrar.
- Realista: regulación exige consistencia "tarifa al momento de entrada".
- **Solución sugerida**: snapshot del valor de tarifa en el ticket al crear (`tarifaValorSnapshot` en `Ticket`).

**G12 — Conciliación diaria**
- No hay reporte automático de "cuadre del día": efectivo recibido vs tickets cerrados vs facturas emitidas.
- **Solución sugerida**: job nocturno que genera un resumen y notifica al operador.

---

## 3. Validación de schemas vs entidades (post-fix)

Después de los arreglos de esta sesión:

| Tabla | Entity ↔ data.sql | Comentario |
|---|---|---|
| `tarifa` | ✅ | 7 columnas nuevas en ALTER TABLE IF NOT EXISTS + UPDATE de nulls |
| `tarifa_franja` | ✅ | Nueva, CREATE TABLE alineado |
| `suscripcion` | ✅ | Nueva, CREATE TABLE alineado |
| `movimiento_saldo` | ✅ | Schema mismatch corregido + ALTERs defensivos |
| `convenio` | ✅ | Nueva, CREATE TABLE alineado |
| `validacion_compra` | ✅ | Nueva, CREATE TABLE alineado |
| `empresa` | ✅ | 2 columnas nuevas + UPDATE |
| `parqueadero` | ✅ | 1 columna nueva |
| `ticket` | ✅ | 2 columnas nuevas |
| `factura` | ✅ | 3 columnas nuevas (baseImponible, ivaMonto, ivaPorcentaje) |
| `pago` | ✅ | Sin cambios estructurales (validación a nivel service) |

**Total**: 11 tablas auditadas, 0 inconsistencias residuales.

---

## 4. Validación de flujos críticos

### 4.1 Cierre de ticket end-to-end (post-v38)

```
Usuario o cámara dispara cierre
  └─ TicketService.registrarSalida (o update con estado=CERRADO)
       └─ CobroOrchestrator.cobrar(ticket, salida)
            └─ Strategy 1: MensualCobroStrategy   (Order 1)   → si activa, suscripcionId, monto=0
            └─ Strategy 2: PaseDiaCobroStrategy   (Order 2)   → si activa, suscripcionId, monto=0
            └─ Strategy 3: AbonoPrepagoStrategy   (Order 3)   → descuenta saldo, ajusta saldo, MovimientoSaldo
            └─ Strategy 4: TarifaNormalStrategy   (Order 99)  → Modelo B + tope legal + franja horaria
       └─ Aplicar descuento Convenio (si ValidacionCompra existe)
       └─ ticket.suscripcionId, montoCalculado, estado=CERRADO
       └─ TicketCerradoEvent publish
             ├─ NotificationEventListener   → WebSocket /topic/parqueadero/{id}
             ├─ AutoFacturaListener (NUEVO) → si monto>0 y sin suscripcion: Factura PENDIENTE
             │       └─ Si tarifa aplicaIva → desagrega baseImponible/ivaMonto/ivaPorcentaje
             └─ (otros listeners de ocupación, etc)
```

**Validado en tests:** 13 de TicketServiceTest + 6 de AutoFacturaListenerTest + 8 de Modelo B + 5 de franjas + 7 de convenio.

### 4.2 Pago end-to-end

```
POST /api/pagos { facturaId, monto, metodo }
  └─ PagoService.save
       ├─ Valida RBAC (ADMIN/SUPER_ADMIN)
       ├─ Valida multi-tenant (factura.parqueadero.empresa)
       ├─ Valida factura no esta PAGADA ni ANULADA
       ├─ Valida monto > 0 y <= saldo pendiente
       ├─ Valida metodo en enum MetodoPago
       └─ Si suma de COMPLETADOs cubre valorTotal:
            └─ Factura.estado = "PAGADA"     [✅ confirmado al front]
```

### 4.3 Validación de compra (convenio)

```
POST /api/convenios/validacion-compra { ticketId, convenioId, montoCompra, folioExterno }
  └─ ValidacionCompraService.registrar
       ├─ Ticket debe estar EN_CURSO
       ├─ Convenio.parqueadero debe matchear Ticket.parqueadero
       ├─ Multi-tenant ADMIN
       └─ Persiste ValidacionCompra (sin descuento aún)

Al cerrar ticket:
  └─ CobroOrchestrator.aplicarConvenio
       ├─ findFirstByTicketIdOrderByFechaAplicacionDesc
       ├─ ConvenioDescuentoCalculator.aplicar (MONTO_FIJO|PORCENTAJE|MINUTOS_GRATIS)
       ├─ Persiste descuentoAplicado en la ValidacionCompra
       └─ Retorna CobroResult con monto reducido
```

---

## 5. Validación de FKs e índices

64 `@JoinColumn` en entidades. Índices críticos verificados:

- ✅ `uniq_ticket_punto_en_curso` (anti-doble-ticket en mismo punto)
- ✅ `uniq_ticket_vehiculo_parqueadero_en_curso` (anti-doble-ticket por vehículo)
- ✅ `uniq_suscripcion_activa` (anti-doble-suscripción activa misma tipo+vehiculo+parqueadero)
- ✅ `idx_factura_estado` (queries de morosos)
- ✅ `idx_suscripcion_vence` (job vencimiento O(log N))
- ✅ `idx_mov_saldo_susc` y `idx_mov_saldo_ticket`
- ✅ `idx_convenio_parqueadero` y `idx_validacion_ticket`

Queries con `@Query` derivadas verificadas — todas usan índices declarados.

---

## 6. Recomendación de roadmap

Prioridad sugerida si decides invertir en cerrar gaps de realismo:

| Sprint | Hallazgos a cerrar |
|---|---|
| 1 (alta) | G1 (anulación pago), G2 (cancelación ticket con motivo), G3 (archivar vehículo) |
| 2 (media-alta) | G4 (tipoMovimiento enum), G7 (suscripción con punto reservado), G8 (notificación al cliente) |
| 3 (media) | G5 (visitante), G11 (snapshot tarifa), G12 (conciliación diaria) |
| Indef | G6 (DIAN/recibo PDF), G9 (pasarela), G10 (métricas op) |

---

## 7. Estado actual (v38)

```
ghcr.io/jbeleno/parqueaderos-api:v38 (= :latest)
- 22 endpoints REST nuevos (sobre baseline v32)
- 11 tablas migradas idempotentemente
- 115 tests unitarios verde
- Smoke E2E validado contra produccion 2026-05-26
- Schemas alineados entre JPA y data.sql
- Flujo de cobro 100% automatizado: cerrar ticket -> factura -> IVA desagregado
```

Cualquier feature de la sección 2 se puede priorizar; ninguna es bloqueante para producción.
