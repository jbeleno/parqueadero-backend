# Frontend — Suscripciones, Cobro avanzado y Reportes

Documentación para el equipo de frontend sobre las **nuevas features** disponibles desde la versión v32 del backend.

**Base URL:** `http://deploy.inmero.co:5445`
**WebSocket:** `ws://deploy.inmero.co:5445/ws` (SockJS + STOMP)
**Versión backend:** v32

---

## TL;DR de lo que cambió

1. **Tarifas con valor mínimo + minutos de gracia + minutos cubiertos por el mínimo.** Modelo B: cobra mínimo durante una ventana, después se suma tarifa normal por exceso.
2. **Suscripciones** (3 tipos): MENSUAL, PASE_DIA, ABONO_PREPAGO.
3. **Saldo prepago** con historial de movimientos auditados.
4. **Cobro automático** prioriza suscripciones sobre tarifa normal.
5. **Dashboard de reportes** con 6 endpoints para gráficas/tablas.
6. **Métricas nuevas** en facturación: tickets totales, vehículos únicos, visitas promedio por vehículo.

---

## 1. Cambios en `Tarifa`

La entidad `Tarifa` ahora tiene 5 campos nuevos:

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `minutosGracia` | int | 0 | Minutos gratuitos al inicio. Si la estadía es ≤ este valor, monto=0. |
| `valorMinimo` | double | 0 | Cobro plano (fee de entrada) que cubre los primeros minutos. |
| `minutosCubiertosPorMinimo` | int | 0 | Cuántos minutos cubre el valor mínimo. |
| `precioMensualidad` | double | null | Precio de una suscripción MENSUAL con esta tarifa. Null = no se ofrece. |
| `precioPaseDia` | double | null | Precio de un PASE_DIA con esta tarifa. Null = no se ofrece. |

### Lógica de cobro (Modelo B)

```
si minutos <= minutosGracia: 0
si minutos <= minutosCubiertosPorMinimo: valorMinimo
si no: valorMinimo + tarifaNormal(minutos - minutosCubiertosPorMinimo)
```

### Ejemplo

Tarifa: `valor=3000`, `unidad=POR_HORA`, `minutosGracia=5`, `valorMinimo=3000`, `minutosCubiertosPorMinimo=30`.

| Estadía | Cobro | Explicación |
|---|---|---|
| 4 min | $0 | Dentro de gracia |
| 5 min | $0 | Justo en gracia (incluido) |
| 15 min | $3,000 | Cubierto por el mínimo |
| 30 min | $3,000 | Justo en el cubrimiento |
| 60 min | $6,000 | $3,000 mínimo + 30 min excedente = 1h redondeada = $3,000 |
| 90 min | $6,000 | $3,000 mínimo + 60 min excedente = 1h = $3,000 |
| 120 min | $9,000 | $3,000 mínimo + 90 min excedente = 2h redondeadas = $6,000 |

### UI sugerida en el editor de tarifa

Agregar al formulario los 5 campos nuevos. Mostrar una calculadora en vivo: el operador ingresa una estadía de ejemplo (ej. "1 hora") y ve el cobro resultante con los valores que está configurando.

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
- **AGOTADA**: solo ABONO_PREPAGO con saldo=0
- **CANCELADA**: ADMIN la canceló

### 2.3 Endpoints

#### Crear suscripción

```
POST /api/suscripciones
Authorization: Bearer <token>
Content-Type: application/json

{
  "vehiculoId": 15,
  "parqueaderoId": 7,
  "tarifaId": 3,
  "tipo": "MENSUAL",       // MENSUAL | PASE_DIA | ABONO_PREPAGO
  "montoPagado": 200000
}
```

**Respuesta 201 Created:**

```json
{
  "success": true,
  "message": "Suscripcion creada",
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

**Errores posibles:**

| Code | Causa |
|---|---|
| `ERR_TARIFA_SIN_MENSUALIDAD` | Tipo MENSUAL pero la Tarifa no tiene `precioMensualidad` configurado |
| `ERR_TARIFA_SIN_PASE_DIA` | Tipo PASE_DIA pero la Tarifa no tiene `precioPaseDia` |
| `ERR_SUSCRIPCION_DUPLICADA` | Ya hay una suscripción ACTIVA del mismo tipo para ese vehículo+parqueadero |

#### Otros endpoints de suscripciones

```
GET    /api/suscripciones/{id}                          → detalle
GET    /api/suscripciones/vehiculo/{vehiculoId}         → todas las del vehículo
GET    /api/suscripciones/parqueadero/{id}/activas      → activas del parqueadero
PATCH  /api/suscripciones/{id}/cancelar?reembolsar=true → cancelar
```

### 2.4 UI sugerida

**Listado**: tabla con columnas placa, tipo (badge), estado (badge), fecha fin, monto, acciones (cancelar).

**Form de creación**:
1. Buscar/seleccionar vehículo (autocomplete por placa)
2. Seleccionar parqueadero
3. Seleccionar tarifa del parqueadero
4. Elegir tipo (MENSUAL | PASE_DIA | ABONO_PREPAGO)
5. Mostrar precio sugerido según `tarifa.precioMensualidad` o `tarifa.precioPaseDia`. Para ABONO_PREPAGO el operador ingresa el monto inicial.
6. POST → mostrar confirmación con fecha de vencimiento calculada

**Vista del cliente**: en su perfil, mostrar "Tus suscripciones activas en cada parqueadero" + saldo prepago disponible.

---

## 3. Saldo prepago (ABONO_PREPAGO)

### 3.1 Cargar saldo

```
POST /api/saldos/abonar
{
  "vehiculoId": 15,
  "parqueaderoId": 7,
  "tarifaId": 3,
  "monto": 50000
}
```

**Comportamiento:**
- Si ya existe una suscripción `ABONO_PREPAGO` ACTIVA → suma al saldo existente (re-activa si estaba AGOTADA).
- Si no existe → crea una nueva con saldo inicial = monto.

**Respuesta 200:** mismo `SuscripcionDTO` que en crear, con `saldoRestante` actualizado.

### 3.2 Historial de movimientos

```
GET /api/saldos/suscripcion/{suscripcionId}/movimientos
```

**Respuesta:**

```json
{
  "data": [
    {
      "id": 5,
      "suscripcionId": 1,
      "monto": -3000,
      "ticketId": 42,
      "saldoResultante": 47000,
      "motivo": "Consumo ticket #42",
      "fecha": "2026-05-26T14:30:00"
    },
    {
      "id": 4,
      "monto": 50000,
      "pagoId": null,
      "saldoResultante": 50000,
      "motivo": "Recarga de saldo",
      "fecha": "2026-05-26T12:00:00"
    }
  ]
}
```

**UI sugerida:** página de detalle de saldo del vehículo con timeline cronológica, saldo actual destacado, botón "Recargar saldo" arriba.

---

## 4. Cómo se cobra ahora (orden de prelación)

Cuando un ticket se **cierra** (manual o por OCR), el backend aplica este orden:

```
1. ¿Hay suscripción MENSUAL activa? → ticket cubierto, monto=0, NO factura
2. ¿Hay suscripción PASE_DIA activa? → idem
3. ¿Hay suscripción ABONO_PREPAGO activa?
     → calcular tarifa normal (Modelo B)
     → descontar del saldo lo que se pueda
     → si saldo alcanzó: monto cobrado al ticket = todo, sin factura pendiente
     → si saldo no alcanzó: factura pendiente por la diferencia
4. Sin suscripción → tarifa normal (Modelo B), factura PENDIENTE
```

### Cambio en `Ticket`

Ticket ahora tiene campo nuevo:

```json
{
  "id": 42,
  ...
  "suscripcionId": 1  // null si fue cobro normal, id de Suscripcion si fue cubierto
}
```

Útil para reportes y para mostrar en la UI "cubierto por mensualidad" en el detalle del ticket.

### Cambio en evento WebSocket `PLACA_DETECTADA`

El campo `mensaje` del evento ahora puede traer info detallada del cobro:

| Mensaje | Acción del front |
|---|---|
| `Salida registrada automaticamente` | Cobro normal, mostrar total |
| `Cubierto por mensualidad (suscripcion #1)` | Badge verde "Mensual" |
| `Cubierto por pase de dia (suscripcion #2)` | Badge azul "Pase de día" |
| `Descontado del saldo prepago (saldo restante: $47000)` | Mostrar nuevo saldo |
| `Saldo prepago insuficiente: descontado $X, pendiente $Y` | Alerta amarilla con monto pendiente |
| `Dentro de minutos de gracia` | Badge gris "Gracia" |

---

## 5. Dashboard de reportes (NUEVO)

Todos los endpoints requieren `ADMIN` o `SUPER_ADMIN` y reciben `parqueaderoId` obligatorio.

### 5.1 Facturación agregada

```
GET /api/reportes/facturacion
  ?parqueaderoId=7
  &desde=2026-05-01
  &hasta=2026-05-31
  &agruparPor=mes      // dia | semana | mes
```

**Respuesta:**

```json
{
  "data": {
    "desde": "2026-05-01",
    "hasta": "2026-05-31",
    "parqueaderoId": 7,
    "agruparPor": "mes",
    "totalFacturado": 1234567,
    "totalPagado": 1100000,
    "totalPendiente": 134567,
    "cantidadFacturas": 245,
    "cantidadTickets": 312,
    "vehiculosUnicos": 87,
    "visitasPromedioPorVehiculo": 3.59,
    "buckets": [
      {
        "periodo": "2026-05-01T00:00:00",
        "facturado": 1234567,
        "pagado": 1100000,
        "pendiente": 134567,
        "cantidadFacturas": 245,
        "cantidadTickets": 312,
        "vehiculosUnicos": 87
      }
    ]
  }
}
```

**UI sugerida:** 4 cards arriba con los totales (facturado/pagado/pendiente/visitas), abajo gráfica de barras o líneas con los buckets.

### 5.2 Facturación por tipo de vehículo

```
GET /api/reportes/facturacion/por-tipo-vehiculo?parqueaderoId=7&desde=Y&hasta=Z
```

Buckets con `periodo` = nombre del tipo ("Carro", "Moto", "Bicicleta") y los mismos campos numéricos.

**UI:** gráfica de pie o donut.

### 5.3 Facturación por fuente (tickets vs suscripciones)

```
GET /api/reportes/facturacion/por-fuente?parqueaderoId=7&desde=Y&hasta=Z
```

Buckets con `periodo` = `"TICKET_NORMAL"` o `"SUSCRIPCION"`.

**UI:** comparativa que muestra cuánto del ingreso viene de cada fuente. Útil para decidir si vale la pena promover más mensualidades.

### 5.4 Top vehículos

```
GET /api/reportes/top-vehiculos?parqueaderoId=7&desde=Y&hasta=Z&limit=10
```

**Respuesta:**

```json
{
  "data": [
    { "vehiculoId": 15, "placa": "KJV807", "cantidadTickets": 42, "totalFacturado": 126000 },
    { "vehiculoId": 18, "placa": "HFZ558", "cantidadTickets": 30, "totalFacturado": 90000 }
  ]
}
```

**UI:** ranking con avatares (placa), barras horizontales.

### 5.5 Comparativa mes actual vs anterior

```
GET /api/reportes/comparativa?parqueaderoId=7&fechaReferencia=2026-05-15
```

**Respuesta:**

```json
{
  "data": {
    "periodoActualDesde": "2026-05-01",
    "periodoActualHasta": "2026-05-31",
    "periodoAnteriorDesde": "2026-04-01",
    "periodoAnteriorHasta": "2026-04-30",
    "facturadoActual": 1234567,
    "facturadoAnterior": 980000,
    "deltaAbsoluto": 254567,
    "deltaPorcentual": 25.98,
    "ticketsActual": 312,
    "ticketsAnterior": 280,
    "deltaTicketsPorcentual": 11.43
  }
}
```

**UI:** dos números grandes con flecha verde/roja según el delta y % de cambio.

### 5.6 Resumen de suscripciones

```
GET /api/reportes/suscripciones/resumen?parqueaderoId=7
```

**Respuesta:**

```json
{
  "data": {
    "parqueaderoId": 7,
    "activas": 45,
    "vencidas": 120,
    "vencenEnSieteDias": 3,
    "mensuales": 30,
    "pasesDia": 5,
    "abonosPrepago": 10,
    "saldoTotalEnAbonos": 450000,
    "ingresosUltimoMes": 6000000
  }
}
```

**UI:** cards con cantidad por tipo, alerta destacada para "vencenEnSieteDias" (recordatorio operacional).

---

## 6. Códigos de error nuevos

| Code | HTTP | Cuándo |
|---|---|---|
| `ERR_TARIFA_SIN_MENSUALIDAD` | 422 | Tarifa sin `precioMensualidad` al crear suscripción MENSUAL |
| `ERR_TARIFA_SIN_PASE_DIA` | 422 | Tarifa sin `precioPaseDia` al crear PASE_DIA |
| `ERR_SUSCRIPCION_DUPLICADA` | 422 | Ya hay suscripción ACTIVA del mismo tipo |
| `ERR_TIPO_NO_SOPORTA_ABONO` | 422 | Intentando abonar a suscripción que no es ABONO_PREPAGO |
| `ERR_TIPO_NO_SOPORTA_DESCUENTO` | 422 | Intentando descontar de tipo distinto a ABONO_PREPAGO |
| `ERR_MONTO_INVALIDO` | 422 | Monto ≤ 0 al abonar |

---

## 7. Recomendaciones de UX

### 7.1 Flujo "entrar al parqueadero" del usuario

Cuando un usuario ve un parqueadero específico en la app, mostrar:

1. Si tiene `MENSUAL` activa: badge verde grande "Tu mensual vence el [fecha]". Mostrar días restantes.
2. Si tiene `ABONO_PREPAGO`: "Tu saldo: $X". Si saldo < $5000: alerta naranja "Recarga saldo pronto".
3. Si no tiene nada: botón "Comprar mensualidad ($X)" y "Cargar saldo prepago".

### 7.2 Recibo del ticket cerrado

Mostrar transparencia del cobro:

```
┌──────────────────────────────┐
│ Ticket #42 cerrado           │
│ Vehículo: KJV807             │
│ Entrada: 14:00               │
│ Salida:  14:45  (45 min)     │
│                              │
│ Cubierto por mensualidad ✓   │
│ Total: $0                    │
└──────────────────────────────┘
```

Si fue prepago:
```
│ Tarifa normal: $3,500        │
│ Descontado de saldo: -$3,500 │
│ Saldo restante: $46,500      │
```

Si excedente:
```
│ Tarifa normal: $5,000        │
│ Descontado de saldo: -$2,000 │
│ Saldo restante: $0           │
│ Pendiente por pagar: $3,000  │  ← factura PENDIENTE en BD
```

### 7.3 Dashboard del operador (admin)

Página principal con widgets:

- **Total del mes**: card grande con `totalFacturado` de mes actual (endpoint comparativa)
- **Delta vs mes anterior**: % de cambio, flecha verde/roja
- **Suscripciones próximas a vencer**: lista de 5 con alerta (endpoint resumen suscripciones)
- **Vehículos únicos del mes**: número grande
- **Gráfica de facturación diaria** (endpoint facturacion agruparPor=dia)
- **Top 5 clientes**: lista (endpoint top-vehiculos)
- **Fuente de ingresos**: pie chart (endpoint por-fuente)

---

## 8. Cosas que NO cambiaron (sigues igual)

Lo siguiente sigue exactamente como antes. Si tu código del front ya consumía estos endpoints, **no hay que tocarlos**:

- `POST /api/auth/login`, `POST /api/auth/refresh`, etc.
- `POST /api/tickets`, `PATCH /api/tickets/{id}/salida`, `PATCH /api/tickets/{id}/punto`
- `POST /api/camaras/{id}/imagen`, `GET /api/camaras/{id}/imagen`
- `POST/PUT /api/parqueaderos/configuracion`
- `POST /api/reservas`, etc.
- WebSocket `/topic/parqueadero/{id}` con todos sus eventos (`TICKET_CREADO`, `TICKET_CERRADO`, `PLACA_DETECTADA`, etc.)

Lo que cambió **internamente** es la lógica de **cómo se calcula el monto** al cerrar ticket — el front no se entera, solo recibe el monto correcto en el evento o en el response.

---

## 9. Resumen de endpoints nuevos

| Método | Path | Auth |
|---|---|---|
| `POST` | `/api/suscripciones` | ADMIN |
| `GET` | `/api/suscripciones/{id}` | ADMIN |
| `GET` | `/api/suscripciones/vehiculo/{vehiculoId}` | ADMIN |
| `GET` | `/api/suscripciones/parqueadero/{id}/activas` | ADMIN |
| `PATCH` | `/api/suscripciones/{id}/cancelar` | ADMIN |
| `POST` | `/api/saldos/abonar` | ADMIN |
| `GET` | `/api/saldos/suscripcion/{id}/movimientos` | ADMIN |
| `GET` | `/api/reportes/facturacion` | ADMIN |
| `GET` | `/api/reportes/facturacion/por-tipo-vehiculo` | ADMIN |
| `GET` | `/api/reportes/facturacion/por-fuente` | ADMIN |
| `GET` | `/api/reportes/top-vehiculos` | ADMIN |
| `GET` | `/api/reportes/comparativa` | ADMIN |
| `GET` | `/api/reportes/suscripciones/resumen` | ADMIN |

**Total: 13 endpoints nuevos.**

---

## Contacto y soporte

- Backend: Jesús Beleño
- Versión actual del backend: `ghcr.io/jbeleno/parqueaderos-api:v32`
- Bugs / dudas: pinguear al backend con `[FRONT]` al inicio del mensaje
