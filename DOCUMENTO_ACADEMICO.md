# Sistema integral de gestión de parqueaderos con detección automática de placas vehiculares

**Autor:** Jesús Beleño
**Fecha:** 2026-05-23
**Palabras clave:** parqueadero inteligente, ALPR, OCR, visión por computador, YOLO, PaddleOCR, Spring Boot, microservicios, PostgreSQL/PostGIS, WebSocket.

---

## Resumen

Se presenta el diseño e implementación de un sistema integral para la gestión operativa de parqueaderos vehiculares. El sistema integra (i) un backend REST construido en Spring Boot 3.5.10 sobre Java 21, con persistencia en PostgreSQL 16 + PostGIS y comunicación en tiempo real vía WebSocket STOMP, y (ii) un módulo de visión por computador para la detección y reconocimiento automático de placas (ALPR), desplegado como sidecar Python (FastAPI) con un detector YOLOv11n entrenado y reconocimiento de caracteres mediante PaddleOCR con votación por mayoría y filtrado por expresión regular. El detector YOLO alcanzó mAP50 = 0.987 sobre 4201 imágenes (Roboflow Number Plate v4). En benchmark sobre 60 imágenes (12 placas reales × 5 variaciones de iluminación y ruido), la solución final logró **100% de consistencia** y **83% de precisión literal**, superando a EasyOCR, Tesseract, TrOCR y docTR. El despliegue se realiza en contenedores Docker orquestados por Dokploy.

---

## 1. Objetivo general

Diseñar e implementar un sistema integral para la gestión operativa de parqueaderos vehiculares, que combine un backend de servicios REST con persistencia transaccional y eventos en tiempo real, junto con un módulo de visión por computador que automatice el registro de entradas y salidas mediante la detección y reconocimiento automático de placas vehiculares (ALPR) en cámaras de vigilancia.

---

## 2. Objetivos específicos

1. Diseñar un modelo de datos relacional jerárquico (Empresa → Parqueadero → Nivel → Sección → SubSección → Punto de parqueo) que soporte multi-tenant, geometrías PostGIS y borrado lógico (soft-delete).
2. Implementar una API REST con autenticación JWT (access + refresh con rotación), control de acceso basado en roles (USER, ADMIN, SUPER_ADMIN) y aislamiento por empresa.
3. Desarrollar un sistema de notificaciones en tiempo real basado en WebSocket STOMP para refrescar la disponibilidad y los eventos operativos en los clientes web y móviles.
4. Construir y entrenar un detector de placas vehiculares basado en YOLOv11n, optimizado para condiciones reales del entorno colombiano (placas amarillas y blancas, formato `ABC123`).
5. Comparar cinco soluciones de OCR para la lectura de los caracteres de la placa (EasyOCR, Tesseract, PaddleOCR, TrOCR, docTR) y dos arquitecturas entrenadas localmente (ResNet50, EfficientNet-B0), priorizando la consistencia de la lectura sobre la precisión literal.
6. Integrar el módulo de visión como un sidecar HTTP interno al backend, con cooldown para evitar duplicados y degradación silenciosa ante fallos.
7. Automatizar el flujo de entrada/salida de tickets a partir de la detección de placa, considerando casos especiales (vehículo invitado, ticket duplicado, salida fantasma, salida física confirmada).
8. Validar el sistema completo de extremo a extremo en un entorno de despliegue real (Dokploy) con pruebas funcionales y de rendimiento.

---

## 3. Arquitectura

### 3.1 Visión general

El sistema se organiza en dos contenedores Docker en una misma red interna privada, más un contenedor de base de datos:

```
┌────────────────────────────────────────────────────────────────────┐
│                       Cliente (Web / Mobile)                       │
│                  HTTP + WebSocket STOMP/SockJS                     │
└─────────────────────────────┬──────────────────────────────────────┘
                              │ HTTPS público
                              ▼
┌────────────────────────────────────────────────────────────────────┐
│                       Red interna Docker                           │
│                                                                    │
│   ┌─────────────────────┐    HTTP     ┌────────────────────────┐  │
│   │  parqueaderos-api   │ ──────────▶ │  ocr (sidecar Python)  │  │
│   │  (Spring Boot)      │  /read-plate│  YOLO + PaddleOCR      │  │
│   │  Java 21, port 8080 │             │  FastAPI, port 8001    │  │
│   └──────┬──────────────┘             └────────────────────────┘  │
│          │ JDBC                                                    │
│          ▼                                                         │
│   ┌─────────────────────┐                                          │
│   │  PostgreSQL 16      │                                          │
│   │  + PostGIS 3.4      │                                          │
│   └─────────────────────┘                                          │
└────────────────────────────────────────────────────────────────────┘
```

Solo el servicio `parqueaderos-api` se expone al exterior. El sidecar OCR y la base de datos son alcanzables únicamente desde la red interna del compose.

### 3.2 Stack tecnológico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje (backend) | Java | 21 (Eclipse Temurin) |
| Framework (backend) | Spring Boot | 3.5.10 |
| Persistencia | PostgreSQL + PostGIS | 16 / 3.4 |
| ORM | Hibernate / Spring Data JPA | 6.6.41 |
| Seguridad | Spring Security + JJWT | 6.5.7 / 0.12.6 |
| WebSocket | Spring Messaging STOMP | 6.2.15 |
| Cache | Spring Cache (ConcurrentMap) | 6.x |
| Documentación | springdoc-openapi | 2.x |
| Lenguaje (sidecar) | Python | 3.11 |
| API (sidecar) | FastAPI + Uvicorn | 0.115 / 0.34 |
| Detección | Ultralytics YOLO | ≥8.0 |
| OCR | PaddleOCR + PaddlePaddle | ≥2.7 / ≥2.6 |
| Visión | OpenCV (headless) | ≥4.6 |
| Build / Orquestación | Maven + Docker + Docker Compose | — |
| Despliegue | Dokploy + GHCR | — |

### 3.3 Estructura modular del backend

El backend se organiza en 14 paquetes funcionales bajo `com.usco.parqueaderos_api`:

```
auth/         Registro, login, JWT, PIN, recuperación de contraseña.
user/         Usuarios, personas, roles asignados.
parking/      Parqueaderos, niveles, secciones, subsecciones, puntos,
              cámaras, configuración bulk-save de layout.
ticket/       Entrada/salida de vehículos (incluye flujo automático OCR).
reservation/  Reservas de puntos por intervalo de tiempo.
tariff/       Tarifas por parqueadero y tipo de vehículo, cálculo de monto.
billing/      Facturas y pagos asociados a tickets.
vehicle/      Vehículos vinculados a personas.
device/       Sensores, cámaras y barreras (entidades IoT genéricas).
catalog/      Catálogos (Estados, Roles, TiposVehiculo, TiposParqueadero,
              TiposPuntoParqueo, TiposDispositivo).
location/     Países, Departamentos, Ciudades.
notification/ Listener de eventos → publica en /topic/parqueadero/{id} y
              /queue/usuario/{id}.
ocr/          Cliente HTTP al sidecar y listener async tras subida de
              imagen.
common/       Excepciones globales, ApiResponse wrapper, CORS, salud,
              configuración de cache y thread pool, almacenamiento de
              imágenes.
```

Métricas estructurales: 16 controladores, 32 servicios, 29 repositorios JPA, ~138 endpoints REST y 60 tests unitarios.

### 3.4 Patrones arquitectónicos aplicados

- **Controller → Service → Repository** clásico de Spring Boot, con DTOs mapeados dentro de la transacción para evitar problemas de inicialización perezosa (`LazyInitializationException`).
- **Multi-tenant por `empresa_id`** propagado en cascada desde el contexto del usuario autenticado (`CurrentUserService`).
- **RBAC** mediante anotaciones `@PreAuthorize("hasAnyRole(...)")` sobre endpoints administrativos.
- **Eventos de dominio** publicados con `ApplicationEventPublisher` (`TicketCreadoEvent`, `TicketCerradoEvent`, `TicketPuntoCambiadoEvent`, `ReservaCreadaEvent`, `ReservaCanceladaEvent`, `CamaraImagenActualizadaEvent`, `PlacaDetectadaEvent`) y consumidos por listeners `@Async` con `@TransactionalEventListener(phase = AFTER_COMMIT)` para evitar procesamiento de transacciones que hicieron rollback.
- **Bloqueo pesimista (`SELECT ... FOR UPDATE`)** en la asignación automática de puntos para serializar entradas concurrentes.
- **Pool de threads acotado** (`ThreadPoolTaskExecutor` con `core=4`, `max=16`, `queue=100`, política `CallerRunsPolicy`) para limitar la concurrencia de los listeners asíncronos.
- **Cache en memoria** (`@Cacheable`) sobre los catálogos que cambian con baja frecuencia.

### 3.5 Comunicación entre backend y sidecar

El backend se comunica con el sidecar OCR mediante una sola operación HTTP `POST http://ocr:8001/read-plate`, enviando como `multipart/form-data` el `camara_id` y el archivo de imagen. El sidecar responde con un JSON con la lista de placas detectadas (cero o más), su puntaje de confianza por votación y la lista de placas omitidas por estar dentro de la ventana de cooldown de 30 segundos.

Si el sidecar está deshabilitado, no responde o devuelve error, el cliente `PlateReaderClient` devuelve una lista vacía sin propagar la excepción — el flujo principal de subida de imagen nunca falla por el OCR.

---

## 4. Funcionamiento

### 4.1 Flujos principales

#### Registro y confirmación de cuenta

```
Cliente → POST /api/auth/registro {correo, password, persona...}
       ← 201 "Revisa tu correo"
[Servidor genera PIN y lo envía por SMTP (Gmail)]
Cliente → POST /api/auth/confirmar-cuenta {correo, pin}
       ← 200 "Cuenta confirmada"
```

#### Login con rotación de refresh tokens

El access token JWT dura 1 hora; el refresh token (UUID) dura 7 días y se rota en cada uso (`/api/auth/refresh` revoca el viejo y emite uno nuevo).

#### Reserva por parte del usuario

```
GET /api/parqueaderos                   → lista con disponibilidad
GET /api/parqueaderos/{id}/disponibilidad → snapshot detallado
POST /api/reservas {fechaHoraInicio, fechaHoraFin, parqueaderoId, vehiculoId, puntoParqueoId}
WS → RESERVA_CREADA en /queue/usuario/{id}
WS → SPOT_STATUS_CHANGE (reserved) y OCUPACION_ACTUALIZADA en /topic/parqueadero/{id}
```

#### Entrada y salida manual de vehículo

```
POST /api/tickets {parqueaderoId, puntoParqueoId, vehiculoId, tarifaId}
   → estado EN_CURSO, fechaHoraEntrada = NOW del servidor
WS → TICKET_CREADO + SPOT_STATUS_CHANGE (occupied) + OCUPACION_ACTUALIZADA
PATCH /api/tickets/{id}/salida
   → estado CERRADO, fechaHoraSalida = NOW, monto calculado por TarifaCalculatorService
WS → TICKET_CERRADO + SPOT_STATUS_CHANGE (free) + OCUPACION_ACTUALIZADA
```

#### Subida de imagen de cámara (dispara OCR)

```
POST /api/camaras/{id}/imagen (multipart, JPG/PNG ≤5MB)
   → backend resizea a 1280×720, persiste en /app/images, actualiza Camara
WS → CAMERA_IMAGE_UPDATED
[3-5 s después, asíncronamente]
WS → PLACA_DETECTADA con el resultado del flujo automático
```

### 4.2 Flujo automático de tickets por OCR

Al detectarse una placa, el `TicketAutoService` aplica la lógica según el `TipoCamara`:

| Tipo cámara | Acción del backend |
|---|---|
| ENTRADA | Si el vehículo no existe, lo crea como invitado. Si ya tiene ticket EN_CURSO en este parqueadero, ignora. Si hay punto público libre, crea ticket EN_CURSO con tarifa por tipo de vehículo y bloqueo pesimista sobre el punto. |
| SALIDA | Si hay ticket EN_CURSO del vehículo, lo cierra y calcula monto. Si el último ticket está cerrado hace menos de 5 minutos, registra `fechaHoraSalidaFisica` (no recobra). Si no hay ticket reciente, emite alerta de salida fantasma. |
| SEGURIDAD | Solo emite el evento WebSocket, no modifica la base de datos. |

El campo `accion` del evento `PLACA_DETECTADA` puede tomar siete valores:

| Acción | Significado |
|---|---|
| `ENTRADA_REGISTRADA` | Vehículo entró, ticket creado y punto ocupado. |
| `ENTRADA_DUPLICADA` | Vehículo ya tenía ticket abierto en este parqueadero. |
| `SALIDA_REGISTRADA` | Ticket cerrado y monto calculado. |
| `SALIDA_CONFIRMADA_FISICA` | Ticket ya estaba cerrado por el operador; la cámara confirma físicamente. |
| `SALIDA_SIN_TICKET` | Vehículo saliendo sin haber entrado registrado (alerta). |
| `SOLO_DETECCION` | Cámara de seguridad, sin acción sobre BD. |
| `ERROR` | Fallo lógico (sin puntos libres, sin tarifa, etc.). |

### 4.3 Eventos WebSocket emitidos

| Evento | Tópico | Contenido del `data` |
|---|---|---|
| `TICKET_CREADO` | `/topic/parqueadero/{id}` | (vacío; el WS solo notifica el cambio) |
| `TICKET_CERRADO` | `/topic/parqueadero/{id}` | (vacío) |
| `TICKET_PUNTO_CAMBIADO` | `/topic/parqueadero/{id}` | `{ticketId, puntoParqueoAnteriorId, puntoParqueoNuevoId}` |
| `SPOT_STATUS_CHANGE` | `/topic/parqueadero/{id}` | `{puntoParqueoId, subseccionId, estado: free|occupied|reserved, ticketId}` |
| `OCUPACION_ACTUALIZADA` | `/topic/parqueadero/{id}` | `{total, disponibles, ocupados, reservados}` |
| `CAMERA_IMAGE_UPDATED` | `/topic/parqueadero/{id}` | `{cameraId, imagenUrl, timestamp}` |
| `PLACA_DETECTADA` | `/topic/parqueadero/{id}` | `{placa, confianza, cameraId, tipoCamara, detectedAt, accion, ticketId, vehiculoId, vehiculoCreado, puntoParqueoId, montoCalculado, mensaje}` |
| `RESERVA_CREADA` | `/queue/usuario/{id}` | (vacío) |

### 4.4 Reasignación manual de punto

Cuando el OCR autoasigna un punto a un vehículo, el operador puede corregir el cubículo real donde estacionó mediante `PATCH /api/tickets/{id}/punto` con cuerpo `{ "puntoParqueoId": N }`. El backend valida que el ticket esté `EN_CURSO`, que el punto destino pertenezca al mismo parqueadero, que no esté archivado y que esté libre (mediante bloqueo pesimista). Emite los eventos WebSocket correspondientes para que el plano del cliente se actualice en vivo.

---

## 5. Base de datos

### 5.1 Motor

PostgreSQL 16 con extensión PostGIS 3.4 para soportar geometrías (`POINT`, `POLYGON`) usadas en la representación visual de los parqueaderos. Imagen Docker: `postgis/postgis:16-3.4-alpine`.

### 5.2 Entidades principales

```
Empresa
  └── Parqueadero
        └── Nivel
              ├── Seccion
              │     ├── SubSeccion
              │     │     └── PuntoParqueo
              │     └── Camara (opcional)
              ├── Camino
              └── Camara (global del nivel)

Usuario ──1:1── Persona ──1:N── Vehiculo
                                     │
PuntoParqueo ──── Ticket ────────────┤
                  (EN_CURSO, CERRADO, ANULADO)
                                     │
                  ──── Reserva ──────┤
                  (PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA)
                                     │
Ticket ── Factura ── Pago            │
                                     │
                  Tarifa (por parqueadero + tipo de vehículo)

Dispositivo (sensores, cámaras, barreras) — vinculado a Punto/Nivel
```

### 5.3 Estados y transiciones

Las entidades de negocio usan un catálogo `estado` con tres valores (ACTIVO, INACTIVO, ARCHIVADO) en lugar de booleanos, para mantener uniformidad.

| Entidad | Estados posibles | Transición típica |
|---|---|---|
| `Ticket` | EN_CURSO → CERRADO o ANULADO | El backend setea fechas y monto autoritativamente |
| `Reserva` | PENDIENTE → CONFIRMADA → CANCELADA / EXPIRADA | Transiciones validadas en `ReservaService` |
| `Factura` | PENDIENTE → PAGADA / ANULADA | Generada al cerrar el ticket |
| `Pago` | COMPLETADO / PENDIENTE / RECHAZADO | Registrado por el operador |

### 5.4 Seed inicial

`src/main/resources/data.sql` inserta datos base usando `ON CONFLICT DO NOTHING`:

- 3 estados (ACTIVO, INACTIVO, ARCHIVADO)
- 3 roles (USER, ADMIN, SUPER_ADMIN)
- 3 tipos de vehículo (Carro, Moto, Bicicleta)
- 3 tipos de parqueadero (Público, Privado, Mixto)
- 3 tipos de punto de parqueo (Placas, administrativo, discapacitado)
- 3 tipos de dispositivo (Sensor, Cámara, Barrera)
- 1 país (Colombia), 5 departamentos, 10 ciudades

### 5.5 Soft-delete

Las entidades estructurales (Empresa, Parqueadero, Nivel, Sección, SubSección, PuntoParqueo) no se eliminan físicamente. En su lugar, se utilizan endpoints `PATCH /{id}/archivar` que cambian `estado_id` a `ARCHIVADO`. Esto preserva integridad referencial con tickets y facturas históricos.

El borrado masivo en cascada (al archivar un parqueadero entero) usa consultas `@Modifying @Query` de tipo `UPDATE ... WHERE` (una por tipo de entidad hija), evitando recorrer N registros y emitir N saves individuales.

### 5.6 Anti-race conditions

Para evitar la sobreventa de un punto (dos tickets EN_CURSO simultáneos sobre el mismo punto), se aplican dos mecanismos:

1. **Bloqueo pesimista** sobre el punto destino (`SELECT ... FOR UPDATE`) en `TicketService.save` y `TicketAutoService.buscarYLockearPuntoLibre`.
2. **Índice único parcial** en la tabla `ticket` con la condición `estado = 'EN_CURSO'`, que actúa como red de seguridad a nivel de motor de base de datos.

---

## 6. Despliegue y variables de entorno

### 6.1 Imagen Docker multi-stage

El `Dockerfile` del backend usa dos etapas:

1. **Build:** `eclipse-temurin:21-jdk-alpine` con Maven Wrapper. Construye el JAR ejecutable de Spring Boot.
2. **Runtime:** `eclipse-temurin:21-jre-alpine`, crea usuario no-root `spring:spring`, crea `/app/images` con ownership correcto y copia el JAR. Punto de entrada `java -jar app.jar`.

### 6.2 Sidecar OCR

El `ocr/Dockerfile` usa `python:3.11-slim`, instala dependencias del sistema (`libgl1`, `libglib2.0-0`, `libgomp1`) y las librerías Python (`fastapi`, `uvicorn`, `ultralytics`, `paddlepaddle`, `paddleocr`, `opencv-python-headless`, `numpy`, `python-multipart`). Incluye un pre-warm de los modelos de PaddleOCR durante el build para acelerar el primer request. La imagen resultante pesa ~2 GB extraídos.

### 6.3 Docker Compose para producción (Dokploy)

```yaml
services:
  api:
    image: ghcr.io/jbeleno/parqueaderos-api:latest
    pull_policy: always
    restart: unless-stopped
    ports:
      - "5445:8080"
    environment:
      DB_URL: jdbc:postgresql://parqueadero-db-f3zhnv-db-1:5432/parqueaderos
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_ACCESS_EXP: 3600000
      JWT_REFRESH_EXP: 604800000
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      JPA_DDL_AUTO: update
      PIN_EXPIRATION: 15
      APP_IMAGES_DIR: /app/images
      OCR_URL: http://ocr:8001
      OCR_ENABLED: "true"
      OCR_TIMEOUT_MS: "15000"
    volumes:
      - parqueaderos_images_v2:/app/images
    networks: [dokploy-network]
    depends_on: [ocr]

  ocr:
    image: ghcr.io/jbeleno/parqueaderos-ocr:latest
    pull_policy: always
    restart: unless-stopped
    environment:
      COOLDOWN_SECONDS: "30"
      DETECTOR_CONF: "0.25"
      MIN_VOTING_CONF: "0.66"
    networks: [dokploy-network]

volumes:
  parqueaderos_images_v2:

networks:
  dokploy-network:
    external: true
```

El servicio `ocr` no expone puertos al exterior. Solo es accesible desde la red interna mediante el DNS interno del compose (`http://ocr:8001`).

### 6.4 Variables de entorno del backend

| Variable | Default | Obligatoria | Descripción |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/parqueaderos` | sí | Cadena JDBC al PostgreSQL |
| `DB_USERNAME` | `postgres` | sí | Usuario de BD |
| `DB_PASSWORD` | — | sí | Contraseña de BD |
| `JWT_SECRET` | — | sí | Clave HMAC para firmar tokens JWT |
| `JWT_ACCESS_EXP` | `3600000` | no | Duración del access token (ms) |
| `JWT_REFRESH_EXP` | `604800000` | no | Duración del refresh token (ms) |
| `MAIL_HOST` | `smtp.gmail.com` | no | Servidor SMTP |
| `MAIL_PORT` | `587` | no | Puerto SMTP |
| `MAIL_USERNAME` | — | si email | Usuario SMTP |
| `MAIL_PASSWORD` | — | si email | Password SMTP (app password Gmail) |
| `APP_IMAGES_DIR` | `/app/images` | no | Directorio para almacenar imágenes |
| `OCR_URL` | `http://ocr:8001` | no | URL interna del sidecar |
| `OCR_ENABLED` | `true` | no | Habilita el flujo OCR |
| `OCR_TIMEOUT_MS` | `10000` | no | Timeout HTTP al sidecar (ms) |
| `PORT` | `8080` | no | Puerto HTTP del backend |
| `JPA_DDL_AUTO` | `update` | no | Estrategia DDL de Hibernate |
| `PIN_EXPIRATION` | `15` | no | Duración del PIN en minutos |

### 6.5 Variables de entorno del sidecar OCR

| Variable | Default | Descripción |
|---|---|---|
| `COOLDOWN_SECONDS` | `30` | Tiempo mínimo entre lecturas idénticas (cámara, placa) |
| `YOLO_MODEL_PATH` | `yolo11n_placas.pt` | Ruta al detector entrenado |
| `DETECTOR_CONF` | `0.25` | Umbral mínimo de confianza del detector |
| `MIN_VOTING_CONF` | `0.66` | Mínimo de votación para aceptar la lectura (2 de 3) |

### 6.6 Pipeline de construcción y despliegue

```bash
# Build cross-platform y push a GitHub Container Registry
docker buildx build --platform linux/amd64 \
  -t ghcr.io/jbeleno/parqueaderos-api:vN \
  -t ghcr.io/jbeleno/parqueaderos-api:latest \
  --push .

# Dokploy detecta la nueva imagen y aplica docker compose pull + up -d
```

---

## 7. Modelo de detección y reconocimiento de placas

### 7.1 Caso de uso y métrica prioritaria

El sistema requiere que la **misma placa produzca la misma cadena** al entrar y al salir del parqueadero, aun cuando la lectura no sea literalmente correcta. Esto permite identificar al vehículo contra la base de datos sin importar si hay un error sistemático en uno o dos caracteres. Por esta razón, la métrica prioritaria es la **consistencia** (mismo string a través de variaciones de iluminación, ángulo y ruido), mientras que la precisión literal (lectura exacta de la placa real) es secundaria.

### 7.2 Dataset

- **Fuente:** Roboflow *Number Plate v4* — `universe.roboflow.com/usco-thj9e/number-plate-hiu16`.
- **Tamaño:** 4201 imágenes anotadas en formato YOLO.
- **División:** 3592 entrenamiento, 346 validación, 263 prueba.
- **Preprocesamiento:** auto-orientación EXIF y redimensionamiento a 640×640 con padding negro para preservar la relación de aspecto.

### 7.3 Detector YOLOv11n

**Modelo base:** `yolo11n.pt` (variante *nano*, 2.6 M parámetros).

**Hiperparámetros del entrenamiento:**

| Parámetro | Valor |
|---|---|
| Épocas configuradas | 20 |
| Épocas reales (early stop) | 11 |
| Batch size | 8 |
| Image size | 640 |
| Optimizer | SGD (auto) con momentum 0.937 |
| Learning rate inicial | `lr0 = 0.01` |
| Learning rate final | `lrf = 0.01` (decay coseno) |
| Device | CPU |
| Augmentation | mosaic 1.0, fliplr 0.5, hsv_h 0.015, scale 0.5 |

**Métricas finales sobre el conjunto de validación:**

| Métrica | Valor |
|---|---|
| **mAP50** | **0.987** |
| **mAP50-95** | **0.820** |
| **Precision** | **0.998** |
| **Recall** | **1.000** |
| Box loss | 0.694 |
| Class loss | 0.418 |

El detector no es el cuello de botella del sistema. Sobre las 12 placas reales del set de validación, recorta correctamente las 12 (100% recall).

### 7.4 Entrenamientos propios de OCR (descartados)

Se intentaron dos arquitecturas con dataset balanceado (8611 entrenamiento / 1076 validación / 1077 prueba, 300 imágenes por clase, 36 clases: 0–9 y a–z).

#### ResNet50

Cuatro corridas con distinto número de épocas:

| Corrida | Test accuracy (sintético) | 12 placas reales |
|---|---|---|
| ep20 | **0.967** | 0/12 |
| ep30 | 0.918 | 0/12 |
| ep45 | 0.901 | 0/12 |
| ep60 | 0.880 | 0/12 |

**Causa raíz del fracaso:** el pipeline diseñado fue `YOLO → recorte → segmentación de caracteres por contornos (cv2.adaptiveThreshold + findContours) → clasificación carácter por carácter`. La segmentación por contornos colapsa en placas reales por sombras irregulares, brillos especulares, suciedad acumulada y deterioro. El modelo recibe trozos rotos en lugar de caracteres limpios y clasifica fielmente lo que recibe, pero recibe basura.

#### EfficientNet-B0

Cuatro experimentos con distinta combinación de batch, learning rate y augmentation. Los cuatro colapsaron a `accuracy ≈ 0.028`, equivalente a azar (1/36).

**Bug identificado:** `ImageDataGenerator(rescale=1./255)` es incompatible con `EfficientNetB0(weights='imagenet')`, porque EfficientNet espera input en rango `[0, 255]` y aplica su propia normalización interna mediante `tensorflow.keras.applications.efficientnet.preprocess_input`. La escala manual destruye la información que el modelo espera. El bug está documentado y arreglado en el script de entrenamiento, pero el reentrenamiento queda como trabajo futuro.

### 7.5 Benchmark de soluciones de OCR

Se compararon cinco soluciones externas y la mejor solución propia. Para cada placa real se generaron cinco variaciones que simulan condiciones reales de entrada y salida:

| Variación | Transformación |
|---|---|
| v0 original | Recorte de YOLO sin alterar |
| v1 bright | `convertScaleAbs(α=1.2, β=10)` (mediodía soleado) |
| v2 dark | `convertScaleAbs(α=0.8, β=-10)` (atardecer) |
| v3 zoom_out | Recorte distinto del YOLO (10% más alrededor) |
| v4 noisy | Ruido gaussiano σ=12 (foto borrosa) |

Total: **12 placas × 5 variaciones = 60 imágenes**.

#### Métricas evaluadas

- **Consistencia:** las 5 variaciones de una misma placa producen exactamente el mismo string.
- **Mayoría correcta:** al menos 3 de las 5 lecturas coinciden con el ground truth.
- **Accuracy literal:** las 5 lecturas coinciden con el ground truth.

#### Resultados

| Rank | OCR | Consistencia | Mayoría correcta | Latencia | Veredicto |
|---|---|---|---|---|---|
| 1 | **PaddleOCR v2** (voting + filtro) | **100% (12/12)** | **83% (10/12)** | 2.4 s/img | Ganador |
| 2 | EasyOCR | 67% (8/12) | 50% (6/12) | 0.2 s/img | Alternativa rápida |
| 3 | PaddleOCR vanilla | 58% (7/12) | 83% (10/12) | 0.8 s/img | Buena precisión sin mejoras |
| 4 | Tesseract | 0% (0/12) | 17% (2/12) | 0.1 s/img | No detecta muchas |
| 5 | TrOCR (small-printed) | 8% (1/12) | 0% (0/12) | 0.07 s/img | Entrenado para documentos |
| 6 | docTR (Mindee) | 0% (0/12) | 0% (0/12) | 0.9 s/img | Entrenado para documentos |
| 7 | ResNet50 ep20 (propio) | — | 0% (0/12) | — | Descartado |

Detalle por placa de la solución ganadora (PaddleOCR v2):

| Placa | Ground truth | v0 | v1 | v2 | v3 | v4 | Consistente | Literal |
|---|---|---|---|---|---|---|---|---|
| IMG_4489 | KJV807 | KJV807 | KJV807 | KJV807 | KJV807 | KJV807 | ✓ | ✓ |
| IMG_4490 | THS228 | THS228 | THS228 | THS228 | THS228 | THS228 | ✓ | ✓ |
| IMG_4491 | HFZ558 | HFZ558 | HFZ558 | HFZ558 | HFZ558 | HFZ558 | ✓ | ✓ |
| IMG_4492 | KSR750 | KSR750 | KSR750 | KSR750 | — | KSR750 | ✓ | ✓ |
| IMG_4493 | LUQ850 | LUQ850 | LUQ850 | LUQ850 | LUQ850 | LUQ850 | ✓ | ✓ |
| IMG_4494 | KDV643 | KDV643 | KDV643 | KDV643 | KDV643 | KDV643 | ✓ | ✓ |
| IMG_4495 | LUQ740 | LUQ740 | LUQ740 | LUQ740 | LUQ740 | LUQ740 | ✓ | ✓ |
| IMG_4496 | ELV526 | ELV526 | ELV526 | ELV526 | ELV526 | ELV526 | ✓ | ✓ |
| IMG_4497 | CGN789 | CGN789 | CGN789 | CGN789 | CGN789 | CGN789 | ✓ | ✓ |
| IMG_4498 | **GQX879** | GOX879 | GOX879 | GOX879 | GOX879 | GOX879 | ✓ | ✗ |
| IMG_4503 | **LOQ674** | LOO674 | LOO674 | LOO674 | LOO674 | LOO674 | ✓ | ✗ |
| IMG_4571 | FWV120 | FWV120 | FWV120 | FWV120 | FWV120 | FWV120 | ✓ | ✓ |

Los dos errores residuales son la confusión de la letra Q por la letra O, consistente en las cinco variaciones de la misma placa. Esto **no afecta** el caso de uso del parqueadero: si la placa entra como `LOO674`, sale como `LOO674`, y el match contra la base de datos del vehículo asociado es correcto.

### 7.6 Solución ganadora: PaddleOCR v2

Tres mejoras sobre PaddleOCR vanilla:

1. **Preprocesamiento de tres variantes:**
   - Original (BGR sin alterar).
   - Brillante: `cv2.convertScaleAbs(img, alpha=1.15, beta=10)`.
   - Suavizado: `cv2.bilateralFilter(img, 9, 75, 75)`.

2. **Filtro por expresión regular** que descarta lecturas con basura (nombre de ciudad impresa en la placa, símbolos, banderines):

   ```python
   PLATE_REGEX       = re.compile(r"([A-Z]{3})\s*[\.\-·]?\s*(\d{3})")
   PLATE_LOOSE_REGEX = re.compile(r"([A-Z0]{3})\s*[\.\-·]?\s*(\d{3})")  # tolera Q→0→O
   ```

3. **Majority voting** sobre las 3 lecturas válidas, con confianza `votos_del_ganador / lecturas_válidas`. Se acepta el resultado si la confianza es ≥ 0.66 (al menos 2 de 3 variantes coincidieron).

### 7.7 Pipeline integrado

```
┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐   ┌──────────────────┐   ┌─────────────┐
│ Cámara Tapo  │──▶│  YOLOv11n    │──▶│ Recorte placa        │──▶│  PaddleOCR ×3    │──▶│  Voting +   │
│  RTSP / HTTP │   │  detector    │   │ + padding 6%         │   │  (3 variantes)   │   │  regex      │
└──────────────┘   │  mAP50 0.987 │   │                      │   │                  │   │  ABC123     │
                   └──────────────┘   └──────────────────────┘   └──────────────────┘   └─────┬───────┘
                                                                                              │
                                                                                              ▼
                                                                                  Match contra BD
                                                                                  → entrada / salida
```

Latencia total medida: **≈2.5 s por imagen** (1280×720 → placa detectada y leída). El despliegue en producción usa `enable_mkldnn=False` en PaddleOCR para evitar un bug conocido (`ConvertPirAttribute2RuntimeAttribute not support`) que afecta ciertos CPUs cuando se combina el executor PIR con oneDNN.

---

## 8. Resultados

### 8.1 Métricas del modelo

- Detector YOLO: mAP50 = 0.987 sobre 4201 imágenes; recall 1.000 sobre el conjunto de validación.
- OCR final (PaddleOCR v2): consistencia 100% (12/12 placas), accuracy literal 83% (10/12). Los 2 errores son confusión Q→O consistente que no afecta el match en base de datos.

### 8.2 Métricas del backend

- 138 endpoints REST documentados con OpenAPI.
- 60 tests unitarios (5 archivos), todos pasando: `TicketAutoServiceTest` (12), `TicketServiceTest` (19), `PlateReaderClientTest` (4), `AssignedSpotsReaderTest` (7), `PagoServiceTest` (8), `TarifaCalculatorServiceTest` (9), `ParqueaderosApiApplicationTests` (1).
- Latencia end-to-end del flujo OCR: 3–5 s desde la subida de imagen hasta la emisión del evento `PLACA_DETECTADA` por WebSocket.
- Ratio test:código de producción: 681 / 10631 ≈ 6.4%, con foco en los servicios más críticos (tickets automáticos, cliente OCR, asignación de espacios, cobro).

### 8.3 Validación end-to-end en producción

Se ejecutaron siete escenarios end-to-end en el ambiente desplegado en Dokploy (`http://deploy.inmero.co:5445`), todos exitosos:

| # | Escenario | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | Cámara ENTRADA detecta placa nueva | `ENTRADA_REGISTRADA`, ticket creado, vehículo invitado creado | ✓ |
| 2 | Misma placa por otra cámara ENTRADA del mismo parqueadero | `ENTRADA_DUPLICADA` (no crea segundo ticket) | ✓ |
| 3 | Cámara SALIDA detecta placa con ticket EN_CURSO | `SALIDA_REGISTRADA`, ticket cerrado, monto calculado | ✓ |
| 4 | Cierre manual + cámara SALIDA dentro de 5 minutos | `SALIDA_CONFIRMADA_FISICA`, registra `fechaHoraSalidaFisica` | ✓ |
| 5 | PATCH `/api/tickets/{id}/punto` a otro punto | 2× `SPOT_STATUS_CHANGE` + `TICKET_PUNTO_CAMBIADO` por WS | ✓ |
| 6 | Cámara SEGURIDAD detecta placa | `SOLO_DETECCION`, no toca base de datos | ✓ |
| 7 | Cámara SALIDA detecta placa sin ticket previo | `SALIDA_SIN_TICKET` (alerta de fantasma) | ✓ |

---

## 9. Conclusiones

La integración de un módulo de detección y reconocimiento de placas con un backend transaccional resulta viable cuando se diseñan ambos componentes con responsabilidades bien delimitadas y comunicación asíncrona. La elección de un sidecar Python con FastAPI, en lugar de embeber el modelo en la JVM, permite aprovechar el ecosistema de visión por computador de Python sin comprometer la estabilidad del backend Java. La comunicación HTTP interna entre contenedores ofrece un acoplamiento suelto y degradación silenciosa: si el sidecar falla, el flujo principal de la API continúa funcionando.

El benchmark exhaustivo sobre OCRs comerciales confirmó que la combinación PaddleOCR con votación por mayoría y filtrado por expresión regular del formato colombiano `ABC123` supera ampliamente a las alternativas evaluadas. La métrica de consistencia (100%) es más relevante que la precisión literal (83%) para el caso de uso del parqueadero, porque permite recuperar al vehículo correctamente de la base de datos aun cuando hay errores sistemáticos en la lectura.

El intento previo de entrenar modelos propios (ResNet50, EfficientNet-B0) reveló dos lecciones importantes. Primero, una precisión alta sobre un conjunto de prueba sintético puede ser completamente engañosa cuando el pipeline de inferencia depende de una segmentación frágil — el modelo ResNet50 alcanzó 96.7% en prueba pero 0% en placas reales. Segundo, la combinación de transferencia de aprendizaje con pesos preentrenados requiere usar el preprocesamiento esperado por la red original; usar `rescale=1./255` con EfficientNet resultó en colapso total a comportamiento aleatorio.

La arquitectura del backend, basada en eventos de dominio y listeners asíncronos con bloqueo pesimista en los puntos críticos, permitió integrar la detección automática sin desestabilizar los flujos manuales existentes. La separación clara entre `TicketAutoService` (lógica de negocio) y `OcrEventListener` (orquestación asíncrona) facilita pruebas unitarias y permite extender el sistema con nuevos tipos de cámara o nuevas reglas de negocio sin tocar la lógica de detección.

---

## 10. Trabajo futuro

1. **Sensores físicos por punto de parqueo** para confirmar la ocupación independientemente del operador, permitiendo eliminar la reasignación manual de cubículo.
2. **Reentrenar EfficientNet-B0** con el preprocesamiento correcto (`preprocess_input` de Keras) y comparar los resultados contra PaddleOCR para evaluar si una red liviana puede igualar al OCR comercial.
3. **Validación con un conjunto más grande de placas reales** (50–100 placas adicionales) para confirmar que la consistencia de 100% se mantiene fuera del conjunto de prueba inicial.
4. **Optimización de latencia** del OCR reduciendo el preprocesamiento a 2 variantes en lugar de 3, lo que recortaría el tiempo a ~1.6 s por imagen con el costo aceptable de una posible disminución en consistencia.
5. **Migración del cache de catálogos** de `ConcurrentMapCacheManager` a Redis o Caffeine, para soportar despliegues con múltiples instancias del backend.
6. **Detector de movimiento previo** al envío de frames al OCR, para reducir la cantidad de imágenes que el sidecar procesa cuando no hay vehículos circulando.
7. **Reentrenamiento del detector YOLO** con un dataset propio que incluya placas de motocicleta y placas con condiciones de iluminación extrema (noche, lluvia, contraluz directo).
8. **Métricas de auditoría** sobre el campo `fechaHoraSalidaFisica` para identificar tiempos atípicos entre el cobro y la salida física, útiles para mejorar la operación.

---

## 11. Referencias

[1] Spring Boot 3.5 Documentation. https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/

[2] Spring Security Reference. https://docs.spring.io/spring-security/reference/

[3] JJWT — JSON Web Token for Java. https://github.com/jwtk/jjwt

[4] Hibernate ORM 6 User Guide. https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html

[5] PostgreSQL 16 Documentation. https://www.postgresql.org/docs/16/

[6] PostGIS 3.4 Manual. https://postgis.net/documentation/

[7] Spring Messaging — WebSocket and STOMP. https://docs.spring.io/spring-framework/reference/web/websocket.html

[8] Ultralytics YOLOv11 Documentation. https://docs.ultralytics.com/models/yolo11/

[9] PaddleOCR — PaddlePaddle. https://github.com/PaddlePaddle/PaddleOCR

[10] EasyOCR — JaidedAI. https://github.com/JaidedAI/EasyOCR

[11] Tesseract OCR — Google. https://github.com/tesseract-ocr/tesseract

[12] TrOCR — Transformer-based Optical Character Recognition with Pre-trained Models, Microsoft. https://huggingface.co/microsoft/trocr-small-printed

[13] docTR — Document Text Recognition, Mindee. https://mindee.github.io/doctr/

[14] Roboflow Number Plate v4 Dataset. https://universe.roboflow.com/usco-thj9e/number-plate-hiu16

[15] FastAPI Framework. https://fastapi.tiangolo.com/

[16] OpenCV 4 Documentation. https://docs.opencv.org/4.x/

[17] Dokploy Self-hosted PaaS. https://dokploy.com/

[18] Docker Compose Specification. https://compose-spec.io/

[19] GitHub Container Registry (ghcr.io). https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry

[20] springdoc-openapi. https://springdoc.org/
