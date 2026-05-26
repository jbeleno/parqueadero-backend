# Frontend — Guía integrada de features del backend

Documentación para el equipo de frontend con **todo lo disponible** desde la
versión v37 del backend (Modelo B, IVA, franjas horarias, tope legal, métodos
de pago, modo formal/informal, NTC 4904, suscripciones, reportes, deuda/morosos,
jobs de notificación, convenios con descuento por compra y borrado seguro de
vehículos).

- **Base URL:** `http://deploy.inmero.co:5445`
- **WebSocket:** `ws://deploy.inmero.co:5445/ws` (SockJS + STOMP, topic `/topic/parqueadero/{id}`)
- **Imagen actual:** `ghcr.io/jbeleno/parqueaderos-api:v37` (= `:latest`)
- **Auth:** todas las rutas (excepto login/health) requieren `Authorization: Bearer <token>`. Wrapper de respuestas: `ApiResponse<T> { success, message, data, errorCode, timestamp }`.

---

## TL;DR de lo que cambió

| Bloque | Qué es | Endpoints / campos clave |
|---|---|---|
| **1. Tarifa** | Modelo B + IVA + tope legal + suscripciones + franjas horarias | `Tarifa` con 9 campos nuevos, `/api/tarifas/franjas/*` |
| **2. Suscripciones** | MENSUAL, PASE_DIA, ABONO_PREPAGO | `/api/suscripciones/*`, `/api/saldos/*` |
| **3. Cobro automático** | Strategy con orden: suscripción → tarifa → descuento por compra | `Ticket.suscripcionId` |
| **4. Reportes** | Dashboard de 6 endpoints | `/api/reportes/*` |
| **5. Deuda / morosos** | Endpoints nuevos | `/api/vehiculos/{id}/deuda`, `/api/vehiculos/morosos` |
| **6. Convenios** | Descuento por compra (ticket de comercio) | `/api/convenios/*` |
| **7. Empresa formal/informal** | NIT + modo de operación | `Empresa.modoOperacion`, `Empresa.nit` |
| **8. Pago** | Enum estricto de métodos | `Pago.metodo` validado |
| **9. Notificaciones** | Vehículo abandonado + suscripción por vencer | WebSocket eventos nuevos |
| **10. Borrado seguro** | Vehículo con historial devuelve 409 legible | `ERR_VEHICULO_CON_HISTORIAL` |

---

## 1. Tarifa — Modelo B + IVA + tope + suscripciones + franjas

### 1.1 Campos nuevos en `Tarifa`

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `minutosGracia` | int | 0 | Minutos gratuitos al inicio. Si estadía ≤ este valor → monto = 0. **Opcional**. |
| `valorMinimo` | double | 0 | Cobro plano que cubre los primeros minutos. **Opcional**. |
| `minutosCubiertosPorMinimo` | int | 0 | Cuántos minutos cubre el `valorMinimo`. **Opcional**. |
| `precioMensualidad` | double | null | Precio de Suscripcion MENSUAL. `null` = no se ofrece. |
| `precioPaseDia` | double | null | Precio de Suscripcion PASE_DIA. `null` = no se ofrece. |
| `aplicaIva` | boolean | false | Si es `true`, el monto cobrado se desagrega en `base + iva`. Parqueaderos formales (DIAN) deben activarlo. |
| `ivaPorcentaje` | double | 0 | Porcentaje de IVA (Colombia formal: 19.0). Solo se usa si `aplicaIva = true`. |

> **Importante:** todos son **opcionales**. Las tarifas viejas siguen funcionando — quedan con defaults `0/false/null` y se comportan como antes.

### 1.2 Unidad de tarifa: nuevos valores

`Tarifa.unidad` ahora soporta **5 valores canónicos**:

| Valor | Lógica | Cuándo usarla |
|---|---|---|
| `POR_MINUTO` | `minutos * valor` (sin redondeo) | Estándar legal Bogotá; cobro lineal por minuto |
| `POR_HORA` | `ceil(minutos / 60) * valor` | Cobro tradicional por hora redondeada hacia arriba |
| `POR_FRACCION` | `ceil(minutos / minutosFraccion) * valor` | Fracciones de 15/30 min |
| `POR_DIA` | `ceil(minutos / 1440) * valor` | Estacionamiento por días |
| `PLANA` | `valor` (independiente del tiempo) | Eventos, parqueo simbólico |

El back es tolerante con variantes ("Hora", "hora", "Por Hora" → `POR_HORA`).

### 1.3 Lógica de cobro (Modelo B + tope + IVA)

```
1. duracion = salida - entrada (en minutos, min 0)
2. si duracion <= minutosGracia → 0
3. si duracion <= minutosCubiertosPorMinimo → valorMinimo
4. excedente = duracion - minutosCubiertosPorMinimo
   cobro = valorMinimo + (excedente cobrado según unidad)
5. si Parqueadero.tarifaMaximaPorMinuto != null:
     tope = tarifaMaximaPorMinuto * (duracion - minutosGracia)
     cobro = min(cobro, tope)              ← protección legal
6. si TarifaFranja activa para la hora de entrada:
     se sustituye valor base por el valor de la franja  ← paso 4 usa este valor
7. si aplicaIva = true:
     respuesta puede desagregar { base, iva, total }
```

### 1.4 Ejemplo completo

Tarifa: `valor=3000`, `unidad=POR_HORA`, `gracia=5`, `mínimo=3000`, `cubre=30`, `aplicaIva=true`, `iva=19%`.

| Estadía | Cobro bruto | Base (sin IVA) | IVA | Explicación |
|---|---|---|---|---|
| 4 min  | $0     | 0       | 0       | Dentro de gracia |
| 15 min | $3,000 | $2,521  | $479    | Cubierto por el mínimo |
| 60 min | $6,000 | $5,042  | $958    | $3,000 mínimo + 30 min excedente (1h redondeada) |
| 90 min | $6,000 | $5,042  | $958    | $3,000 + 60 min excedente (1h) |
| 120 min | $9,000 | $7,563 | $1,437  | $3,000 + 90 min excedente (2h redondeadas) |

### 1.5 Tope legal por parqueadero

`Parqueadero` tiene nuevo campo:

| Campo | Tipo | Descripción |
|---|---|---|
| `tarifaMaximaPorMinuto` | double / null | Tope regulado (ej. Bogotá ~$50/min). Si la tarifa configurada genera un cobro mayor al tope × minutos cobrables, el sistema lo **trunca al tope**. `null` = sin tope. |

UI: agregar este campo en el editor de Parqueadero. Mostrar warning si una tarifa del parqueadero excede el tope al calcular cobros típicos.

### 1.6 Franjas horarias (`TarifaFranja`)

Permite que una tarifa cobre distinto según la hora de entrada (nocturna, peak, fin de semana).

**Entidad:**

```json
{
  "id": 1,
  "tarifaId": 3,
  "nombre": "NOCTURNA",
  "horaInicio": "22:00:00",
  "horaFin": "06:00:00",         // si horaFin <= horaInicio cruza medianoche
  "valor": 1500.0,                // sustituye al valor base de la tarifa
  "soloFinesDeSemana": false,
  "activa": true
}
```

**Endpoints:**

```
GET    /api/tarifas/franjas/por-tarifa/{tarifaId}
POST   /api/tarifas/franjas                          (ADMIN)
DELETE /api/tarifas/franjas/{id}                     (ADMIN)
```

**Selección**: si la hora de entrada cae en alguna franja activa, ese valor reemplaza al `valor` base de la tarifa para el cobro. Si hay varias y matchean, se usa la primera.

**UI sugerida:** agregar pestaña "Franjas horarias" en el editor de tarifa. Mostrar visualmente las franjas en un reloj de 24h o lista de períodos.

### 1.7 POST/PUT tarifa completo (front)

```http
POST /api/tarifas
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "Carro día completo",
  "valor": 3000,
  "unidad": "POR_HORA",
  "minutosFraccion": null,
  "parqueaderoId": 3,
  "tipoVehiculoId": 1,

  // Modelo B (opcionales)
  "minutosGracia": 10,
  "valorMinimo": 2000,
  "minutosCubiertosPorMinimo": 30,

  // IVA (opcional)
  "aplicaIva": true,
  "ivaPorcentaje": 19.0,

  // Suscripciones (opcionales, null = no se ofrece)
  "precioMensualidad": 150000,
  "precioPaseDia": 10000
}
```

**Respuesta 201** trae **los mismos campos** que se enviaron, más `id`, `parqueaderoNombre`, `tipoVehiculoNombre`.

---

## 2. Suscripciones

### 2.1 Tipos

| Tipo | Vigencia | Comportamiento |
|---|---|---|
| `MENSUAL` | 30 días desde compra | Vehículo entra/sale libre sin generar factura |
| `PASE_DIA` | 24 horas desde compra | Igual que MENSUAL pero más corto |
| `ABONO_PREPAGO` | 365 días | Saldo en $ que se descuenta por cada salida. Si saldo no alcanza, queda factura pendiente por la diferencia |

### 2.2 Estados

`PENDIENTE` → `ACTIVA` → (`VENCIDA` | `AGOTADA` | `CANCELADA`)

- **PENDIENTE**: creada pero pago no confirmado
- **ACTIVA**: vigente, da derechos
- **VENCIDA**: pasó `fechaFin`. Marcado automático por job @Scheduled cada hora
- **AGOTADA**: solo ABONO_PREPAGO con saldo = 0
- **CANCELADA**: ADMIN la canceló

### 2.3 Endpoints

```http
POST   /api/suscripciones
GET    /api/suscripciones/{id}
GET    /api/suscripciones/vehiculo/{vehiculoId}
GET    /api/suscripciones/parqueadero/{id}/activas
PATCH  /api/suscripciones/{id}/cancelar?reembolsar=true
```

#### POST body

```json
{
  "vehiculoId": 15,
  "parqueaderoId": 7,
  "tarifaId": 3,
  "tipo": "MENSUAL",
  "montoPagado": 200000
}
```

**Respuesta 201:**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "vehiculoId": 15,
    "vehiculoPlaca": "KJV807",
    "parqueaderoId": 7,
    "parqueaderoNombre": "USCO PRUEBA",
    "tarifaId": 3,
    "tipo": "MENSUAL",
    "estado": "ACTIVA",
    "fechaInicio": "2026-05-26T12:00:00",
    "fechaFin": "2026-06-25T12:00:00",
    "montoPagado": 200000,
    "saldoRestante": null,
    "fechaCreacion": "2026-05-26T12:00:00"
  }
}
```

**Errores:**

| Code | Causa |
|---|---|
| `ERR_TARIFA_SIN_MENSUALIDAD` | Tipo MENSUAL pero la tarifa no tiene `precioMensualidad` |
| `ERR_TARIFA_SIN_PASE_DIA` | Tipo PASE_DIA pero la tarifa no tiene `precioPaseDia` |
| `ERR_SUSCRIPCION_DUPLICADA` | Ya hay suscripción ACTIVA del mismo tipo para (vehículo, parqueadero) |

### 2.4 Saldo prepago

```http
POST /api/saldos/abonar
{ "vehiculoId": 15, "parqueaderoId": 7, "tarifaId": 3, "monto": 50000 }

GET /api/saldos/suscripcion/{suscripcionId}/movimientos
```

**Movimientos**: timeline inmutable (event sourcing). Cada abono y consumo deja registro auditado.

```json
{
  "data": [
    { "id": 5, "monto": -3000, "ticketId": 42, "saldoResultante": 47000, "motivo": "Consumo ticket #42", "fecha": "..." },
    { "id": 4, "monto": 50000, "saldoResultante": 50000, "motivo": "Recarga de saldo", "fecha": "..." }
  ]
}
```

---

## 3. Cobro automático (orden de prelación)

Cuando un ticket se cierra (manual o por OCR), el backend evalúa estrategias **en orden**:

```
1. MENSUAL activa     → ticket cubierto, monto = 0, NO factura
2. PASE_DIA activa    → idem
3. ABONO_PREPAGO activa con saldo > 0 →
     calcular tarifa normal (Modelo B)
     descontar del saldo lo que se pueda
     si alcanza: monto cobrado, sin factura
     si no:      monto parcial + factura PENDIENTE por la diferencia
4. Tarifa normal      → Modelo B + IVA, factura PENDIENTE
5. Convenio (descuento por compra) ← se aplica DESPUÉS si hay ValidacionCompra
```

### Campo nuevo en `Ticket`

```json
{
  "id": 42,
  ...
  "suscripcionId": 1,                   // null = cobro normal
  "fechaHoraSalidaFisica": "...",       // cuándo la cámara confirmó la salida
  "montoCalculado": 0                   // ya neto del descuento si aplica
}
```

### Eventos WebSocket — campo `mensaje` del `PLACA_DETECTADA`

| Mensaje contiene | Caso | Acción UI |
|---|---|---|
| `Cobro con tarifa normal` | Tarifa normal | Mostrar total |
| `Cubierto por mensualidad (suscripcion #X)` | MENSUAL | Badge verde "Mensual" |
| `Cubierto por pase de dia (suscripcion #X)` | PASE_DIA | Badge azul "Pase de día" |
| `Descontado del saldo prepago (saldo restante: $X)` | ABONO con saldo suficiente | Mostrar nuevo saldo |
| `Saldo prepago insuficiente: descontado $X, pendiente $Y` | ABONO sin saldo | Alerta + factura pendiente |
| `Dentro de minutos de gracia` | Gracia | Badge gris "Gracia" |
| `... (descuento convenio: -$X)` | Hubo convenio aplicado | Mostrar descuento |

---

## 4. Reportes (dashboard)

Todos requieren `ADMIN` o `SUPER_ADMIN` y reciben `parqueaderoId` obligatorio.

| Endpoint | Para qué |
|---|---|
| `GET /api/reportes/facturacion?desde&hasta&agruparPor=mes\|semana\|dia` | Totales y serie temporal |
| `GET /api/reportes/facturacion/por-tipo-vehiculo` | Pie por Carro/Moto/Bici |
| `GET /api/reportes/facturacion/por-fuente` | TICKET_NORMAL vs SUSCRIPCION |
| `GET /api/reportes/top-vehiculos?limit=10` | Ranking de clientes |
| `GET /api/reportes/comparativa?fechaReferencia` | Mes actual vs anterior con delta |
| `GET /api/reportes/suscripciones/resumen` | Activas, por vencer, saldos |

### 4.1 Facturación agregada

```http
GET /api/reportes/facturacion?parqueaderoId=7&desde=2026-05-01&hasta=2026-05-31&agruparPor=mes
```

```json
{
  "data": {
    "totalFacturado": 1234567,
    "totalPagado": 1100000,
    "totalPendiente": 134567,
    "cantidadFacturas": 245,
    "cantidadTickets": 312,
    "vehiculosUnicos": 87,
    "visitasPromedioPorVehiculo": 3.59,
    "buckets": [
      { "periodo": "2026-05-01T00:00:00", "facturado": 1234567, "pagado": 1100000,
        "pendiente": 134567, "cantidadFacturas": 245, "cantidadTickets": 312, "vehiculosUnicos": 87 }
    ]
  }
}
```

### 4.2 Resumen suscripciones

```json
{
  "data": {
    "activas": 45, "vencidas": 120, "vencenEnSieteDias": 3,
    "mensuales": 30, "pasesDia": 5, "abonosPrepago": 10,
    "saldoTotalEnAbonos": 450000, "ingresosUltimoMes": 6000000
  }
}
```

**UI sugerida** (dashboard del operador):
- Cards arriba: totalFacturado, delta vs mes anterior (verde/rojo), vehículos únicos, suscripciones activas
- Gráfica de barras de los buckets
- Pie de fuente de ingresos (TICKET vs SUSCRIPCION)
- Lista top 5 clientes
- Alerta "X suscripciones vencen en los próximos 7 días"

---

## 5. Deudas y morosos (NUEVO)

### 5.1 Deuda detallada por vehículo

```http
GET /api/vehiculos/{id}/deuda
```

```json
{
  "success": true,
  "data": {
    "vehiculoId": 28,
    "placa": "KXW386",
    "totalAdeudado": 18500,
    "cantidadFacturas": 3,
    "facturasPendientes": [
      { "id": 21, "valorTotal": 5000, "estado": "PENDIENTE", "fechaHora": "...", "parqueaderoNombre": "..." },
      ...
    ]
  }
}
```

**RBAC**:
- USER → solo ve sus propios vehículos (de su persona)
- ADMIN → ve cualquier vehículo, pero **solo facturas de su empresa**
- SUPER_ADMIN → todo

### 5.2 Lista de morosos del operador

```http
GET /api/vehiculos/morosos
```

Devuelve la lista agrupada por vehículo, **ordenada por monto desc**. ADMIN solo ve los de su empresa.

```json
{
  "data": [
    { "vehiculoId": 18, "placa": "HFZ558", "totalAdeudado": 90000, "cantidadFacturas": 5, "facturasPendientes": [...] },
    { "vehiculoId": 15, "placa": "KJV807", "totalAdeudado": 12000, "cantidadFacturas": 2, "facturasPendientes": [...] }
  ]
}
```

**UI sugerida**:
- Sección "Cobros pendientes" en el dashboard
- Botón "Cobrar ahora" → POST `/api/pagos` con `facturaId` y método (ver §6)
- Marca visual de severidad por monto/edad de la deuda más vieja

---

## 6. Pagos con método validado

`POST /api/pagos` ya validaba antes, pero ahora `metodo` es un **enum estricto** del lado del backend:

```
EFECTIVO, TARJETA_CREDITO, TARJETA_DEBITO,
NEQUI, DAVIPLATA, PSE, TRANSFERENCIA,
FLYPASS, QR_BANCOLOMBIA, OTRO
```

El front debe enviar uno de estos valores **exactamente** (case-insensitive, el back normaliza a uppercase).

**Errores:**

| Code | HTTP | Causa |
|---|---|---|
| `ERR_INVALID_PAYMENT_METHOD` | 422 | Método fuera del enum |
| `ERR_INSUFFICIENT_FUNDS` | 422 | Monto excede saldo pendiente de la factura |
| `ERR_INVOICE_ALREADY_PAID` | 409 | Factura ya está PAGADA |

> **Confirmación importante para el front**: cuando la suma de pagos COMPLETADOs cubre `valorTotal`, el backend **sí cambia** automáticamente `Factura.estado` de `PENDIENTE` a `PAGADA`. No hay que hacerlo desde el front.

---

## 7. Convenios — descuento por compra (NUEVO)

Modelo común en centros comerciales: el cliente presenta un ticket de compra del comercio y obtiene descuento en el parqueo.

### 7.1 Crear convenio

```http
POST /api/convenios
{
  "parqueaderoId": 7,
  "nombreComercio": "Supermercado X",
  "nitComercio": "900111222-3",
  "tipoDescuento": "PORCENTAJE",      // MONTO_FIJO | PORCENTAJE | MINUTOS_GRATIS
  "porcentajeDescuento": 50.0,
  "valorDescuento": null,             // solo si MONTO_FIJO
  "minutosGratis": null,              // solo si MINUTOS_GRATIS
  "montoMinimoCompra": 30000,
  "fechaInicioVigencia": "2026-05-01T00:00:00",
  "fechaFinVigencia": "2026-12-31T23:59:59"
}
```

| Tipo | Cómo descuenta | Campos requeridos |
|---|---|---|
| `MONTO_FIJO` | `monto = max(0, monto - valorDescuento)` | `valorDescuento` > 0 |
| `PORCENTAJE` | `monto = monto * (1 - %/100)` | `porcentajeDescuento` (1-100) |
| `MINUTOS_GRATIS` | proporcional: si los minutos cubiertos son ≥ duración → gratis; sino se prorratea | `minutosGratis` > 0 |

Si `montoMinimoCompra` está definido y el comprobante es menor → descuento = 0.

### 7.2 Validar compra sobre un ticket en curso

Cuando el cliente trae el comprobante, el operador hace:

```http
POST /api/convenios/validacion-compra
{
  "ticketId": 44,
  "convenioId": 9,
  "montoCompra": 75000,
  "folioExterno": "FT-12345"
}
```

El descuento se aplica **automáticamente** al cerrar el ticket (no en este momento). La respuesta devuelve la `ValidacionCompra` registrada. Al cierre del ticket, `descuentoAplicado` se llena con el monto descontado real.

### 7.3 Otros endpoints

```http
GET   /api/convenios/por-parqueadero/{id}
PATCH /api/convenios/{id}/desactivar           (soft-disable)
```

### 7.4 UI sugerida

- En el detalle del ticket EN_CURSO: botón "Validar compra del comercio" → modal con lista de convenios activos del parqueadero
- Operador selecciona convenio, ingresa monto del recibo y folio, confirma
- En el cierre del ticket, mostrar línea "Descuento por convenio (Supermercado X): -$X,XXX"

---

## 8. Empresa: modo formal vs informal

`Empresa` tiene dos campos nuevos:

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `modoOperacion` | string | `"INFORMAL"` | `FORMAL` (DIAN, IVA, factura electrónica futura) o `INFORMAL` (parqueadero de barrio, recibo simple) |
| `nit` | string | null | NIT/RUT. Obligatorio si `modoOperacion = FORMAL` (validado a nivel UI por ahora) |

UI: al crear/editar Empresa, mostrar toggle "Operación formal" que activa el campo NIT y desbloquea `aplicaIva` en las tarifas asociadas.

---

## 9. Notificaciones WebSocket (eventos nuevos)

Topic: `/topic/parqueadero/{parqueaderoId}` (el front ya está suscrito).

| `tipo` | Cuándo | `data` |
|---|---|---|
| `VEHICULO_ABANDONADO` | Job diario 03:00 detecta tickets EN_CURSO > 30 días | `{ ticketId, placa, entrada, horasEnParqueadero, umbralDias }` |
| `SUSCRIPCION_PROXIMA_A_VENCER` | Job diario 09:00 detecta suscripciones que vencen en ≤ 3 días | `{ suscripcionId, tipo, placa, fechaFin, diasRestantes }` |

Estos jobs son **informativos** — no cierran tickets ni renuevan suscripciones; solo emiten alerta para que el operador decida.

UI sugerida:
- Centro de notificaciones con badges por tipo
- Para `VEHICULO_ABANDONADO`: card con botón "Ver ticket" + "Marcar como contactado" (local en el front)
- Para `SUSCRIPCION_PROXIMA_A_VENCER`: card con botón "Renovar mensualidad" → POST nueva suscripción

---

## 10. Borrado seguro de vehículo (NUEVO 409)

`DELETE /api/vehiculos/{id}` ahora **valida** que el vehículo no tenga historial antes de borrar.

**Si tiene tickets/reservas/facturas:**

```http
HTTP/1.1 409 Conflict
{
  "success": false,
  "message": "No se puede eliminar el vehiculo: tiene historial (3 tickets, 0 reservas, 1 facturas). Reasigne o archive el dato.",
  "errorCode": "ERR_VEHICULO_CON_HISTORIAL"
}
```

**Sin historial:** `204 No Content` y el vehículo se borra.

**Red de seguridad global**: cualquier otro FK/UNIQUE violation no anticipado en el sistema ahora devuelve `409 ERR_DATA_INTEGRITY` con mensaje legible en vez de un `500` opaco.

UI sugerida: al recibir `ERR_VEHICULO_CON_HISTORIAL`, mostrar el mensaje y ofrecer "Reasignar a otra persona" en lugar de eliminar.

---

## 11. NTC 4904 — Validación 2% puntos para discapacitados

Endpoint informativo (no bloquea, solo reporta):

```http
GET /api/parqueaderos/{id}/ntc4904
```

```json
{
  "data": {
    "totalPuntos": 150,
    "puntosDiscapacitados": 2,
    "requeridosMinimo": 3,
    "porcentajeActual": 0.0133,
    "cumple": false,
    "mensaje": "NO cumple NTC 4904: requiere 3, tiene 2"
  }
}
```

*(El endpoint se expone si se decide integrarlo en el front; si no aparece en ENDPOINTS.md actual, el service está listo y se publica al exponer el controller).*

UI sugerida: en la vista del parqueadero, banner con cumplimiento NTC 4904 (verde/amarillo según `cumple`).

---

## 12. Códigos de error nuevos / actualizados

| Code | HTTP | Cuándo |
|---|---|---|
| `ERR_TARIFA_SIN_MENSUALIDAD` | 422 | Tarifa sin `precioMensualidad` al crear suscripción MENSUAL |
| `ERR_TARIFA_SIN_PASE_DIA` | 422 | Tarifa sin `precioPaseDia` al crear PASE_DIA |
| `ERR_SUSCRIPCION_DUPLICADA` | 422 | Ya hay suscripción ACTIVA del mismo tipo |
| `ERR_TIPO_NO_SOPORTA_ABONO` | 422 | Abonar a suscripción que no es ABONO_PREPAGO |
| `ERR_TIPO_NO_SOPORTA_DESCUENTO` | 422 | Descontar de tipo distinto a ABONO_PREPAGO |
| `ERR_MONTO_INVALIDO` | 422 | Monto ≤ 0 |
| `ERR_INVALID_PAYMENT_METHOD` | 422 | `metodo` fuera del enum |
| `ERR_INVALID_TIPO_DESCUENTO` | 422 | `tipoDescuento` no es MONTO_FIJO/PORCENTAJE/MINUTOS_GRATIS |
| `ERR_INVALID_VALUE` | 422 | Valor de campo fuera de rango (% > 100, descuento ≤ 0, etc) |
| `ERR_CONVENIO_PARQUEADERO_MISMATCH` | 422 | Validación de compra con convenio de otro parqueadero |
| `ERR_TICKET_ESTADO` | 422 | Validación de compra sobre ticket no EN_CURSO |
| `ERR_VEHICULO_CON_HISTORIAL` | 409 | Intento de borrar vehículo con tickets/reservas/facturas |
| `ERR_DATA_INTEGRITY` | 409 | Red de seguridad genérica para FK/UNIQUE no anticipados |
| `ERR_INVOICE_ALREADY_PAID` | 409 | Factura ya PAGADA |
| `ERR_INSUFFICIENT_FUNDS` | 422 | Pago > saldo pendiente |

---

## 13. Recomendaciones de UX clave

### 13.1 Editor de tarifa

Pestañas:
1. **Básico**: nombre, valor, unidad (POR_MINUTO recomendado para informales urbanos), minutos fracción si aplica
2. **Modelo B** (opcional): minutos gracia, valor mínimo, minutos cubiertos
3. **IVA** (opcional): toggle "Empresa formal" → activar IVA + porcentaje
4. **Suscripciones** (opcional): precio mensualidad, precio pase de día
5. **Franjas horarias**: lista con CRUD

Calculadora en vivo: "Estadía de prueba" → muestra cobro con toda la config.

### 13.2 Dashboard del operador

Widgets (todos los endpoints del §4):

- **Card grande**: total facturado del mes + delta vs anterior
- **Vehículos únicos del mes** (vista de fidelización)
- **Suscripciones próximas a vencer** (lista clickeable) ← alerta amarilla si > 0
- **Gráfica diaria de facturación**
- **Top 5 clientes**
- **Fuente de ingresos** (TICKET vs SUSCRIPCION)
- **Cobros pendientes**: link a `/morosos`

### 13.3 Vista del cliente (USER)

En su perfil + por parqueadero:

- Suscripciones activas con días/saldo restantes
- Si `MENSUAL`: badge verde "Tu mensual vence el [fecha]"
- Si `ABONO_PREPAGO` con saldo < $5,000: alerta naranja "Recarga pronto"
- Botones "Comprar mensualidad" / "Cargar saldo" (POST a suscripciones / saldos)
- Lista de **mis facturas pendientes** con botón "Pagar"

### 13.4 Recibo del ticket cerrado

Mostrar transparencia del cobro:

```
┌──────────────────────────────────┐
│ Ticket #42 cerrado               │
│ Vehículo: KJV807                 │
│ Entrada: 14:00 — Salida: 14:45   │
│                                  │
│ Tarifa normal:        $5,000     │
│ - IVA (19%):          $   798    │
│ - Descuento convenio: -$2,500    │
│ - Saldo prepago:      -$1,500    │
│ ----                             │
│ Total cobrado:         $1,000    │
│ Saldo restante:        $48,500   │
│ Factura pendiente:     —         │
└──────────────────────────────────┘
```

---

## 14. Resumen completo de endpoints

### Suscripciones / saldos
| Método | Path | Auth |
|---|---|---|
| `POST` | `/api/suscripciones` | ADMIN |
| `GET` | `/api/suscripciones/{id}` | ADMIN |
| `GET` | `/api/suscripciones/vehiculo/{vehiculoId}` | ADMIN |
| `GET` | `/api/suscripciones/parqueadero/{id}/activas` | ADMIN |
| `PATCH` | `/api/suscripciones/{id}/cancelar` | ADMIN |
| `POST` | `/api/saldos/abonar` | ADMIN |
| `GET` | `/api/saldos/suscripcion/{id}/movimientos` | ADMIN |

### Reportes
| Método | Path | Auth |
|---|---|---|
| `GET` | `/api/reportes/facturacion` | ADMIN |
| `GET` | `/api/reportes/facturacion/por-tipo-vehiculo` | ADMIN |
| `GET` | `/api/reportes/facturacion/por-fuente` | ADMIN |
| `GET` | `/api/reportes/top-vehiculos` | ADMIN |
| `GET` | `/api/reportes/comparativa` | ADMIN |
| `GET` | `/api/reportes/suscripciones/resumen` | ADMIN |

### Tarifas — franjas
| Método | Path | Auth |
|---|---|---|
| `GET` | `/api/tarifas/franjas/por-tarifa/{id}` | cualquier auth |
| `POST` | `/api/tarifas/franjas` | ADMIN |
| `DELETE` | `/api/tarifas/franjas/{id}` | ADMIN |

### Deudas / morosos
| Método | Path | Auth |
|---|---|---|
| `GET` | `/api/vehiculos/{id}/deuda` | USER (suyo) / ADMIN / SUPER |
| `GET` | `/api/vehiculos/morosos` | ADMIN |

### Convenios
| Método | Path | Auth |
|---|---|---|
| `GET` | `/api/convenios/por-parqueadero/{id}` | cualquier auth |
| `POST` | `/api/convenios` | ADMIN |
| `PATCH` | `/api/convenios/{id}/desactivar` | ADMIN |
| `POST` | `/api/convenios/validacion-compra` | ADMIN |

**Total nuevo vs documento anterior: 7 endpoints adicionales (4 convenios + 2 deudas + 1 franjas POST/DELETE/GET = 3 franjas). Total acumulado en este doc: 21 endpoints.**

---

## 15. Cosas que NO cambiaron (sigues igual)

- `POST /api/auth/login`, `POST /api/auth/refresh`, etc.
- `POST /api/tickets`, `PATCH /api/tickets/{id}/salida`, `PATCH /api/tickets/{id}/punto`
- `POST /api/camaras/{id}/imagen`, `GET /api/camaras/{id}/imagen`
- `POST/PUT /api/parqueaderos/configuracion`
- `POST /api/reservas`, etc.
- WebSocket `/topic/parqueadero/{id}` con todos sus eventos previos (`TICKET_CREADO`, `TICKET_CERRADO`, `PLACA_DETECTADA`, etc.)

Lo que cambió **internamente** es la lógica de **cómo se calcula el monto** al cerrar ticket — el front no se entera, solo recibe el monto correcto en el evento o en la respuesta del PATCH/POST.

---

## 16. Smoke tests de v37 (validados en producción)

| # | Test | Resultado |
|---|---|---|
| 1 | `GET /api/tarifas` | 200 con los 7 campos Modelo B en cada tarifa |
| 2 | `POST /api/tarifas` con Modelo B + IVA + suscripciones | 201 con los 7 campos exactos persistidos y devueltos |
| 3 | `GET /api/tickets` | 200 con la lista de tickets |
| 4 | `DELETE /api/vehiculos/{id}` con historial | 409 `ERR_VEHICULO_CON_HISTORIAL` legible |
| 5 | `GET /api/vehiculos/morosos` | 200 con la lista |
| 6 | `GET /api/vehiculos/{id}/deuda` | 200 con detalle |

108 tests unitarios verde en CI local. Smoke E2E ejecutado contra `deploy.inmero.co:5445` el 2026-05-26.

---

## Contacto y soporte

- Backend: Jesús Beleño
- Versión actual: `ghcr.io/jbeleno/parqueaderos-api:v37` (= `:latest`)
- Bugs / dudas: pinguear al backend con `[FRONT]` al inicio del mensaje y mencionar siempre:
  - endpoint exacto + método
  - request (payload) y response completos
  - timestamp del log (UTC) para que pueda buscarlo en Dokploy
