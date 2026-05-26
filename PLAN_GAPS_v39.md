# Plan de cierre de gaps (post-v38)

Plan accionable para cerrar los 12 gaps identificados en la auditoría
[AUDITORIA_v38.md](AUDITORIA_v38.md). Cada gap viene con: caso de uso real,
schema, endpoints, archivos a tocar, tests mínimos, y esfuerzo estimado
(tallas T-shirt: S = <2h, M = 2-4h, L = 4-8h).

Orden recomendado: hacer Sprint 1 completo (G1, G2, G3) antes de pasar al
siguiente; cada gap es independiente y se puede mergear por separado.

---

## Sprint 1 — Alta severidad (operación real ya los necesita)

### G1 — Anulación / reverso de pago

**Caso real**: cliente pagó con tarjeta el 15; el banco hace chargeback el 17.
La factura debe volver a `PENDIENTE`. Hoy no hay flujo: el SUPER_ADMIN tiene
que editar a mano en BD.

**Schema**:
```sql
ALTER TABLE pago ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(300);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS anulado_por_usuario_id BIGINT REFERENCES usuario(id);
```

**Endpoint**:
```
PATCH /api/pagos/{id}/anular?motivo=...
Auth: SUPER_ADMIN
Body: { "motivo": "Reverso del banco - chargeback Tx#ABC" }
```

**Lógica**:
1. Solo SUPER_ADMIN, multi-tenant.
2. Pago.estado pasa a `FALLIDO`. Setea `motivoAnulacion`, `anuladoEn`, `anuladoPorUsuarioId`.
3. Recalcula la factura: suma pagos `COMPLETADO` restantes. Si < `valorTotal` → factura vuelve a `PENDIENTE`. Si sigue cubriendo → factura sigue `PAGADA`.
4. Emite evento `PagoAnuladoEvent` → notifica WebSocket.

**Archivos**:
- `Pago.java` (+ 3 campos)
- `PagoService.anular(Long id, String motivo)` (nuevo método)
- `PagoController.anular` (nuevo endpoint)
- `data.sql` (3 ALTER TABLE)
- `PagoServiceTest`: test "anular pago COMPLETADO devuelve factura a PENDIENTE", "USER no puede anular", "ya anulado falla"

**Esfuerzo**: **M** (3h)

---

### G2 — Cancelación de ticket con motivo

**Caso real**: cliente perdió el ticket físico, o el operador detectó que
el OCR registró 2 entradas por el mismo carro. Necesita anular con
justificación auditable.

**Schema**:
```sql
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(300);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMP;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS anulado_por_usuario_id BIGINT REFERENCES usuario(id);
```

**Endpoint**:
```
PATCH /api/tickets/{id}/anular
Auth: ADMIN/SUPER_ADMIN
Body: { "motivo": "Cliente perdio ticket fisico - se cobra con tarifa plana" }
```

**Lógica**:
- Ticket debe estar `EN_CURSO`.
- Setea `estado=ANULADO`, `motivoAnulacion`, `anuladoEn`, `anuladoPorUsuarioId`.
- **Importante**: si hay Factura PENDIENTE asociada, anularla también (`Factura.estado = ANULADA`).
- Liberar el punto (publica `TicketCerradoEvent` para que `ParkingOccupancyListener` actualice).
- `motivo` obligatorio mínimo 10 caracteres (validación BusinessException).

**Archivos**:
- `Ticket.java` (+3 campos)
- `TicketService.anular(Long id, String motivo)` (nuevo método público)
- `TicketController.anular` (endpoint)
- `data.sql` (3 ALTER TABLE)
- `TicketServiceTest`: tests de validación motivo, anula factura asociada, no se puede anular CERRADO

**Esfuerzo**: **M** (3h)

---

### G3 — Soft-delete de vehículo (archivado)

**Caso real**: cliente vende su carro. Operador quiere que el vehículo
deje de aparecer en sugerencias del front pero **mantener el historial**
de tickets y facturas. Hoy `DELETE` bloquea con 409, no hay alternativa.

**Schema**:
```sql
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS archivado_en TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_vehiculo_activo ON vehiculo (activo);
```

**Endpoint**:
```
PATCH /api/vehiculos/{id}/archivar       → activo=false
PATCH /api/vehiculos/{id}/desarchivar    → activo=true
GET   /api/vehiculos?incluirArchivados=true   (default false)
```

**Lógica**:
- `Vehiculo.activo = true` por defecto.
- `VehiculoService.findAll()` filtra `activo=true` salvo que pase `incluirArchivados=true`.
- `findByPlaca` para flujo OCR sigue retornando archivados (no debe crear duplicado si la placa archivada vuelve).
- **Si archivado pero tiene ticket EN_CURSO**: desarchivar automáticamente o lanzar error explicativo.

**Archivos**:
- `Vehiculo.java` (+2 campos)
- `VehiculoRepository`: `findByActivoTrue`, query con flag
- `VehiculoService.archivar/desarchivar` + filtro en findAll
- `VehiculoController.archivar/desarchivar`
- `data.sql`
- Tests

**Esfuerzo**: **M** (3h)

---

### G4 — Enum `TipoMovimiento` en MovimientoSaldo

**Caso real**: hoy `motivo` es texto libre ("Recarga de saldo", "Consumo ticket #42").
Para reportes financieros se necesita filtrar por categoría: ABONO del cliente vs
AJUSTE manual del operador vs REVERSO por anulación de ticket.

**Schema**:
```sql
ALTER TABLE movimiento_saldo ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);
-- Backfill: heuristica por monto
UPDATE movimiento_saldo SET tipo = CASE
  WHEN tipo IS NOT NULL THEN tipo
  WHEN monto > 0 THEN 'ABONO'
  WHEN monto < 0 THEN 'CONSUMO'
  ELSE 'AJUSTE'
END;
```

**Nuevo enum Java**:
```java
public enum TipoMovimiento {
    ABONO,        // cliente carga saldo
    CONSUMO,      // descuento por ticket
    REVERSO,      // revertir consumo (anular ticket que descontó)
    AJUSTE        // ajuste manual del operador (perdida/regalo)
}
```

**Archivos**:
- `MovimientoSaldo.java` (+`@Enumerated(EnumType.STRING) tipo`)
- `SaldoService.abonar`: tipo=ABONO. `descontar`: tipo=CONSUMO.
- Nuevo `POST /api/saldos/ajustar` SUPER_ADMIN (motivo+monto, tipo=AJUSTE)
- Backfill SQL en `data.sql`
- Tests

**Esfuerzo**: **S** (2h)

---

## Sprint 2 — Media-alta severidad

### G5 — Vehículo visitante / guest

**Caso real**: parqueaderos públicos (mall, hospital) atienden ~80%
vehículos únicos. Hoy se crean vehículos "fantasma" sin persona, sin
forma de distinguirlos.

**Schema**:
```sql
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS es_visitante BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE vehiculo ADD COLUMN IF NOT EXISTS ultima_actividad TIMESTAMP;
```

**Lógica**:
- `TicketAutoService` (OCR) cuando crea vehículo sin persona → marca `esVisitante=true`.
- `Vehiculo.ultimaActividad` se actualiza con cada ticket (entrada o salida) — útil para limpieza.
- Endpoint opcional: `POST /api/vehiculos/limpiar-visitantes-inactivos?dias=180` (SUPER_ADMIN).

**Endpoint**:
```
GET /api/vehiculos?soloVisitantes=true|false   (filtro)
```

**Archivos**:
- `Vehiculo.java`
- `TicketAutoService` (marca al crear)
- Listener `TicketCreadoEvent` / `TicketCerradoEvent` que actualiza `ultimaActividad`
- `VehiculoService.limpiarVisitantesInactivos` job o endpoint
- Tests

**Esfuerzo**: **M** (3h)

---

### G6 — Recibo del ticket (PDF/imagen)

**Caso real**: ya cubierto por el front (arma recibo con DTO). Para
imprimir en impresora térmica conviene una representación canónica del
back que sea idéntica entre clientes web/móvil.

**Solución técnica**:
- `GET /api/facturas/{id}/recibo.txt` (formato 80 columnas, fuente fija)
- O un endpoint que genere PDF simple con OpenPDF/iText

**Estado**: usuario pidió "todo menos DIAN/factura electrónica". El recibo
simple **NO** es factura electrónica DIAN. Se puede implementar.

**Schema**: ninguno.

**Archivos**:
- Dep: `com.lowagie:openpdf:2.0.x` o `org.apache.pdfbox:pdfbox:3.x`
- `ReciboService.generarTxt(Long facturaId)` → String
- `ReciboService.generarPdf(Long facturaId)` → byte[]
- `FacturaController` con dos GETs
- Tests de contenido

**Esfuerzo**: **L** (5h) — el formato/branding lleva tiempo

---

### G7 — Suscripción con punto reservado

**Caso real**: oficinas/colegios/conjuntos pagan mensualidad pero quieren
**punto fijo asignado** (ej. "celda 12 es de la oficina X"). Hoy la
suscripción MENSUAL solo da derecho a entrar/salir, no garantiza punto.

**Schema**:
```sql
ALTER TABLE suscripcion ADD COLUMN IF NOT EXISTS punto_parqueo_reservado_id BIGINT
    REFERENCES punto_parqueo(id);
```

**Lógica**:
- Al crear suscripción con `puntoParqueoReservadoId`, el punto queda
  **bloqueado** para uso externo durante la vigencia.
- `TicketAutoService` (OCR) cuando ve la placa de la suscripción
  → asigna automáticamente el punto reservado.
- Otra placa intentando ingresar a un punto reservado → 409
  `ERR_PUNTO_RESERVADO_SUSCRIPCION`.
- Vista del punto: si `getReservadoPor` retorna una suscripción activa, mostrar dueño en UI.

**Conflictos**: ¿qué pasa si el punto reservado está ocupado cuando el dueño
llega? → soft-policy: el operador decide (notificación). Hard-policy
(bloqueo total) genera fricción.

**Archivos**:
- `Suscripcion.java`
- `SuscripcionService.save` (valida punto disponible si se pide)
- `SuscripcionRepository.findReservadoPor(puntoId)` (la consulta)
- `TicketService.create` y `TicketAutoService.crearTicket`: chequear reserva
- Tests

**Esfuerzo**: **L** (6h) — afecta varios flujos

---

### G8 — Notificación al cliente USER

**Caso real**: el cliente USER necesita saber "tu mensualidad vence en 3 días",
"tu saldo se acabó", "se cerró tu ticket". Hoy las notificaciones WebSocket van
al operador (topic del parqueadero), nunca al USER.

**Infra ya existe**: `NotificationService.notificarUsuario(usuarioId, ...)`
publica en `/queue/usuario/{usuarioId}`. El front necesita suscribirse.

**Cambios**:
1. **SuscripcionAvisoVencimientoJob** ahora notifica también al `usuario` dueño:
   ```java
   notificationService.notificarUsuario(usuario.getId(), avisoDto);
   ```
2. Cuando `Suscripcion.saldoRestante` baja de un umbral (ej. < $5000) tras un
   consumo en `AbonoPrepagoCobroStrategy`: emitir evento `SaldoBajoEvent` →
   listener que notifica al usuario.
3. Cuando se cierra un ticket de un cliente con cuenta: notificar.

**Endpoint extra (opcional)**:
```
POST /api/notificaciones/test    SUPER_ADMIN
{ "usuarioId": 5, "tipo": "DEBUG", "mensaje": "ping" }
```

**Archivos**:
- `SuscripcionAvisoVencimientoJob` (modificar)
- Listener nuevo `SaldoBajoEventListener`
- `AbonoPrepagoCobroStrategy` (emitir evento si saldo cruza umbral)
- Tests

**Esfuerzo**: **M** (4h)

---

## Sprint 3 — Media severidad

### G9 — Pagos en línea (link de pago / pasarela)

**Caso real**: cliente quiere pagar su deuda desde la app sin ir al
parqueadero. Integración con PSE/Wompi/Mercado Pago.

**Diseño** (sin tocar pasarela aún):
1. Tabla `pago_intent` con: `factura_id`, `monto`, `referenciaExterna`,
   `estado` (CREADO, COMPLETADO, FALLIDO), `urlPago`, `webhookData`.
2. Endpoint `POST /api/pagos/link-pago { facturaId, monto }` → devuelve
   `urlPago` (stub local por ahora, integración real después).
3. Endpoint `POST /api/pagos/webhook/{proveedor}` (idempotente por
   referencia externa) — confirma → crea `Pago` real, llama a la lógica
   de cierre de factura.

**Esfuerzo MVP (stub)**: **L** (6h)
**Esfuerzo integración real (Wompi)**: 1-2 días extra

---

### G10 — Métricas operacionales (tiempos, ocupación)

**Caso real**: el operador quiere saber "hora pico de mi parqueadero",
"duración promedio de estadía", "% ocupación".

**Schema**: ninguno (calcula sobre tabla `ticket`).

**Endpoints**:
```
GET /api/reportes/operacion/duracion?parqueaderoId=...&desde=...&hasta=...
  → { p50: 47min, p75: 95min, p95: 240min, promedio: 78min }

GET /api/reportes/operacion/ocupacion-horaria?parqueaderoId=...&fecha=...
  → bucket por hora del día: { hora: 14, tickets: 23, promedioOcupacion: 0.72 }
```

**Archivos**: `ReporteService` (queries nativas con `EXTRACT(HOUR FROM ...)`)

**Esfuerzo**: **M** (4h)

---

### G11 — Snapshot de tarifa al crear ticket

**Caso real**: regulación exige consistencia. Si un admin cambia
`Tarifa.valor` mientras hay tickets EN_CURSO, esos tickets verán el
valor nuevo al cerrar — eso puede ser fraude o disputa.

**Schema**:
```sql
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_valor_snapshot DOUBLE PRECISION;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_unidad_snapshot VARCHAR(50);
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_minimo_snapshot DOUBLE PRECISION;
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS tarifa_gracia_snapshot INTEGER;
```

**Lógica**:
- Al crear ticket: copiar todos los campos económicos de la tarifa al ticket.
- `TarifaCalculatorService.calcular`: leer snapshot si existe, sino caer al
  `ticket.getTarifa()` (compat con tickets viejos).
- Cambiar la firma para aceptar Ticket completo (ya la tiene).

**Archivos**:
- `Ticket.java` (+4 campos)
- `TicketService.save` y `TicketAutoService.crearTicket`: setear snapshots
- `TarifaCalculatorService.calcular`: leer snapshots en lugar de tarifa actual
- `data.sql`
- Tests

**Esfuerzo**: **M** (4h) — afecta el calculador, ojo con regresiones

---

### G12 — Conciliación diaria

**Caso real**: cierre del día del operador. ¿Cuánto efectivo debe tener
en la caja? ¿Coincide con lo registrado en el sistema?

**Lógica**:
- Job a las 23:55 que genera un `CierreDia` por parqueadero:
  ```
  { parqueaderoId, fecha, ticketsCerrados, totalCobrado,
    porMetodo: { EFECTIVO: 50000, NEQUI: 30000, ... },
    facturasEmitidas, totalPendiente, anulaciones }
  ```
- Persistir en tabla `cierre_dia` para histórico.
- WebSocket notifica al operador con el resumen.
- Endpoint `GET /api/reportes/cierre-dia?parqueaderoId=...&fecha=...`.

**Schema**:
```sql
CREATE TABLE cierre_dia (
  id BIGSERIAL PRIMARY KEY,
  parqueadero_id BIGINT NOT NULL REFERENCES parqueadero(id),
  fecha DATE NOT NULL,
  tickets_cerrados INTEGER,
  total_cobrado DOUBLE PRECISION,
  total_efectivo DOUBLE PRECISION,
  total_tarjeta DOUBLE PRECISION,
  total_otros DOUBLE PRECISION,
  facturas_emitidas INTEGER,
  total_pendiente DOUBLE PRECISION,
  ticketsanulados INTEGER,
  generado_en TIMESTAMP DEFAULT NOW(),
  UNIQUE(parqueadero_id, fecha)
);
```

**Archivos**: entidad, repo, servicio, job @Scheduled, endpoint, tests.

**Esfuerzo**: **L** (6h)

---

## Tabla resumen

| # | Gap | Severidad | Esfuerzo | Sprint |
|---|---|---|---|---|
| G1 | Anulación de pago | ALTA | M (3h) | 1 |
| G2 | Cancelación ticket con motivo | ALTA | M (3h) | 1 |
| G3 | Soft-delete vehículo | ALTA | M (3h) | 1 |
| G4 | TipoMovimiento enum | ALTA | S (2h) | 1 |
| G5 | Vehículo visitante | MEDIA-ALTA | M (3h) | 2 |
| G6 | Recibo PDF/txt | MEDIA-ALTA | L (5h) | 2 |
| G7 | Suscripción con punto reservado | MEDIA-ALTA | L (6h) | 2 |
| G8 | Notificación al cliente USER | MEDIA-ALTA | M (4h) | 2 |
| G9 | Pago en línea (link/pasarela) | MEDIA | L (6h) | 3 |
| G10 | Métricas operacionales | MEDIA | M (4h) | 3 |
| G11 | Snapshot tarifa en ticket | MEDIA | M (4h) | 3 |
| G12 | Conciliación diaria | MEDIA | L (6h) | 3 |

**Sprint 1**: 11h (los 4 más críticos, hacen falta para operación cotidiana)
**Sprint 2**: 18h (mejoran realismo y experiencia del cliente)
**Sprint 3**: 20h (madurez operativa y financiera)

**Total**: 49h ≈ 1 semana de trabajo concentrado.

---

## Estrategia de implementación por sprint

### Sprint 1 (recomendado YA)

Razón: los 4 son operaciones del día a día. Sin ellos el operador termina
editando BD a mano (G1, G2) o lidiando con vehículos zombies (G3).

Orden sugerido dentro del sprint:
1. **G4** (2h) — más simple, agarras impulso, sin breaking changes.
2. **G3** (3h) — independiente, mejora UX inmediatamente.
3. **G1** (3h) — requiere repensar el recálculo de factura, hazlo con cabeza fresca.
4. **G2** (3h) — similar a G1, reusa patrón.

Después del sprint 1: hacer smoke E2E completo + documento al front con los
4 endpoints nuevos.

### Sprint 2

Más complejo. **G7** (suscripción con punto) afecta varios servicios y
merece testing E2E. **G8** (notificación cliente) requiere coordinar con el
front para que se suscriban al `/queue/usuario/{id}`.

Orden:
1. **G5** (3h) — solo backend, sin coordinación.
2. **G8** (4h) — front necesita suscribir nueva queue.
3. **G6** (5h) — depende de qué formato pide el negocio (térmico vs PDF).
4. **G7** (6h) — lo más arriesgado, dejarlo último para tener todo lo demás estable.

### Sprint 3

**G11** (snapshot) es importante de hacer **antes** de tener datos
financieros pesados — si esperas mucho, la migración para backfillear
snapshots de tickets viejos se complica.

Orden:
1. **G11** (4h) — primero, antes de acumular más historial.
2. **G10** (4h) — solo lectura, sin riesgo.
3. **G12** (6h) — usa los snapshots de G11.
4. **G9** (6h) — depende del proveedor que elijas, puede tomar más.

---

## Patrones reutilizables que ya tienes

Cuando implementes cada gap, sigue los patrones del repo:

- **Entidad + DTO + Service + Controller**: mira `convenio/` como ejemplo.
- **RBAC**: `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` + `currentUser.requireEmpresa(...)`.
- **Multi-tenant**: filtrar por `empresa.id` salvo SUPER_ADMIN.
- **Eventos**: `@EventListener` o `@TransactionalEventListener(AFTER_COMMIT)`. Para listeners que crean nuevos datos, usar `@Transactional(propagation = REQUIRES_NEW)` para no rollearse junto al original.
- **Migración idempotente**: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` con `DEFAULT` literal en SQL, y `nullable` permisivo en JPA al inicio. Si quieres NOT NULL eventual, hazlo en una segunda migración tras el backfill.
- **Tests**: unit tests con Mockito de cada Service nuevo. Mockear `CurrentUserService` con `when(user.isSuperAdmin()).thenReturn(...)`.
- **Mensajes de error**: `BusinessException("mensaje legible", "ERR_CODIGO")`. Añadir el code al `GlobalExceptionHandler.mapBusinessToStatus` si necesita HTTP custom.

---

## Cómo medir éxito por gap

Cada gap debe poder responderse con un comando curl:

**G1**: `PATCH /api/pagos/{id}/anular` → factura vuelve a PENDIENTE si suma < total.
**G2**: `PATCH /api/tickets/{id}/anular` → ticket ANULADO + factura ANULADA si existía.
**G3**: `PATCH /api/vehiculos/{id}/archivar` → siguiente `GET /api/vehiculos` no lo trae.
**G4**: `GET /api/saldos/.../movimientos` → cada movimiento trae `tipo` enum.
**G5**: nuevo vehículo creado por OCR trae `esVisitante=true`.
**G6**: `GET /api/facturas/{id}/recibo.txt` → 200 con texto formateado.
**G7**: POST ticket en punto reservado por otra placa → 409 `ERR_PUNTO_RESERVADO_SUSCRIPCION`.
**G8**: cliente con `MENSUAL` vence en 3 días → recibe push en su `/queue/usuario/{id}`.
**G9**: `POST /api/pagos/link-pago` → 200 con `urlPago` válida.
**G10**: `GET /api/reportes/operacion/duracion` → percentiles correctos.
**G11**: cerrar ticket viejo con tarifa cambiada → cobra con valor de entrada.
**G12**: `GET /api/reportes/cierre-dia?fecha=ayer` → resumen completo del día.

---

## Riesgos y mitigaciones

| Gap | Riesgo | Mitigación |
|---|---|---|
| G1, G2 | Rollback inconsistente entre Pago y Factura | Toda la lógica en una transacción + tests específicos del recálculo |
| G3 | Front no soporta el nuevo flag → muestra archivados como activos | Coordinar con front antes de mergear, default `incluirArchivados=false` no rompe nada |
| G4 | Datos viejos sin `tipo` | Backfill SQL en la migración (UPDATE con CASE) |
| G7 | Reserva colisiona con tickets EN_CURSO en el momento del cambio | Validar al crear suscripción: si el punto está ocupado, error claro |
| G11 | Tickets viejos sin snapshot | Calculador con fallback a `ticket.getTarifa()` actual (acepta histórico inconsistente conscientemente) |
| G12 | Job nocturno corre 2 veces en un día | UNIQUE (parqueadero, fecha) + INSERT … ON CONFLICT DO NOTHING |

---

## Contacto

- Backend: Jesús Beleño
- Auditoría origen: [AUDITORIA_v38.md](AUDITORIA_v38.md)
- Plan: este documento, [PLAN_GAPS_v39.md](PLAN_GAPS_v39.md)
