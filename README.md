# 🚗 Parqueaderos API

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-3.4-336791?style=flat-square)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Container Registry](https://img.shields.io/badge/GHCR-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/jbeleno/parqueadero-backend/pkgs/container/parqueaderos-api)
[![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)

REST API para sistema de gestión de parqueaderos con disponibilidad en tiempo real, autenticación JWT, OCR de placas, facturación DIAN y consultas geoespaciales con PostGIS.

> Proyecto académico desarrollado durante pasantías en la **Universidad Surcolombiana (USCO)**. Backend de producción con autenticación, websockets, geolocalización, multi-tenant por empresa y despliegue dockerizado en Dokploy.

---

## 🌐 Producción

| Recurso | URL / Valor |
|---|---|
| **API base** | `https://parqueadero-api-useune-c1a095-158-69-200-27.traefik.me` (Dokploy + Traefik.me) |
| **Swagger UI** | `<API_BASE>/swagger-ui.html` |
| **Health check** | `<API_BASE>/api/health` |
| **Imagen Docker** | `ghcr.io/jbeleno/parqueaderos-api:latest` |
| **Tag actual** | `v49` (digest `sha256:83fd18ac...`) |
| **Plataforma** | `linux/amd64` (build multi-arch via `buildx`) |
| **Orquestador** | Dokploy (Docker Compose detrás de Traefik) |
| **BD** | PostgreSQL 16 + PostGIS 3.4 autoalojado en Dokploy |
| **Sidecar OCR** | `ghcr.io/jbeleno/parqueaderos-ocr:latest` (YOLO11n + PaddleOCR) |

### Usuarios de prueba

| Correo | Password | Rol |
|---|---|---|
| `admin@test.com` | `admin123` | `ADMIN` |
| `super@test.com` | `admin123` | `SUPER_ADMIN` |

> **Login body:** `{"correo": "...", "password": "..."}` (el campo es `correo`, NO `email`).
> **Wrapper:** Todas las respuestas usan `ApiResponse<T>` con `success`, `message`, `data`, `timestamp`.

---

## ✨ Overview

Sistema backend completo para administrar parqueaderos comerciales: jerarquía Empresa → Parqueadero → Nivel → Sección → SubSección → PuntoParqueo, gestión multi-tenant de usuarios y vehículos, tickets con OCR automático, reservas, tarifas configurables, facturación DIAN multi-resolución, pagos, suscripciones (mensual/pase-día/abono prepago), convenios comerciales, caja del operador con arqueo, dispositivos IoT (sensores, cámaras), auditoría universal y reportes parametrizables.

La API expone **~160 endpoints REST** documentados con OpenAPI/Swagger y soporta actualizaciones de disponibilidad **en tiempo real** mediante WebSocket + STOMP.

## 🎯 Key Features

- 🔐 **JWT + RBAC granular** con 5 roles (USER, ADMIN, SUPER_ADMIN, ADMIN_PARQUEADERO, OPERARIO_CAJA).
- 🏢 **Multi-tenant por empresa** con scope automático en cada query.
- 📡 **WebSocket + STOMP** para disponibilidad en vivo.
- 🗺️ **PostGIS** con Hibernate Spatial para búsquedas por proximidad.
- 📷 **OCR de placas** vía sidecar Python (YOLO11n + PaddleOCR voting).
- 🧾 **Facturación DIAN** con resoluciones, consecutivos, IVA desagregado, backfill.
- ⏱️ **Tarifas Modelo B** con franjas horarias, valor mínimo, tope, gracia.
- 💳 **Multi-método de pago** con anulación trazable y arqueo de caja.
- 📊 **Reportes parametrizables** editables sin recompilar (v49 Fase 8).
- ⚙️ **Configuración por empresa** key-value con 26 settings y validaciones editables.
- 📜 **Auditoría universal** con `@Auditable` AOP + `audit_log` append-only.
- 📚 **OpenAPI / Swagger UI** auto-documentado.
- ☁️ **Dockerizado multi-arch** + push automático a GHCR.

## 🛠️ Stack

| Categoría | Tecnologías |
|---|---|
| Lenguaje | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 3.5.10 |
| Persistencia | Spring Data JPA · Hibernate 6.6 · Hibernate Spatial |
| Base de datos | PostgreSQL 16 + PostGIS 3.4 |
| Seguridad | Spring Security · JJWT 0.12.6 |
| Real-time | Spring WebSocket + STOMP |
| Email | Resend API (HTTP) |
| OCR | Sidecar Python (Ultralytics YOLO11n + PaddleOCR) |
| Documentación | SpringDoc OpenAPI 2.8 |
| Build | Maven (wrapper) · Docker buildx |
| Registry | GitHub Container Registry (`ghcr.io`) |
| Deploy | Dokploy (Docker Compose + Traefik) |
| Otros | Lombok · Bean Validation · Cache (ConcurrentMap) |

## 📊 Métricas actuales (v49)

| Métrica | Valor |
|---|---|
| Controladores REST | **34** |
| Endpoints REST | **~160** |
| Servicios de dominio | **55** |
| Repositorios JPA | **62** |
| Tablas de negocio | **41+** (post v49: +23 nuevas) |
| Tests | **139 @Test** ✅ |
| Imágenes Docker | `:v49` = `:latest` |

---

## 📦 Build y publicación Docker

### Build local + push a GHCR (`linux/amd64`)

```bash
# 1. Login a GHCR (una vez, requiere PAT con scope write:packages)
echo $GHCR_PAT | docker login ghcr.io -u jbeleno --password-stdin

# 2. Build multi-arch + push directo
docker buildx build --platform linux/amd64 \
  -t ghcr.io/jbeleno/parqueaderos-api:latest \
  -t ghcr.io/jbeleno/parqueaderos-api:v49 \
  --push .
```

El `Dockerfile` es multi-stage:
- **Stage 1 (build):** `eclipse-temurin:21-jdk-alpine`, ejecuta `./mvnw clean package -DskipTests`.
- **Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine`, corre como usuario `spring` non-root, expone `8080`, monta `/app/images` para almacenamiento de imágenes.

### Workflow típico de deploy

```bash
# 1. Tests verdes
./mvnw test          # debe pasar 139/139

# 2. Build de imagen + push
docker buildx build --platform linux/amd64 \
  -t ghcr.io/jbeleno/parqueaderos-api:latest \
  -t ghcr.io/jbeleno/parqueaderos-api:vXX \
  --push .

# 3. Commit + push a main
git add -A && git commit -m "..." && git push

# 4. Dokploy → Redeploy del servicio api
#    (la migración SQL en data.sql se aplica automáticamente al arrancar)
```

---

## ☁️ Despliegue en Dokploy

La app vive en **Dokploy** detrás de Traefik. La configuración es Docker Compose-compatible.

### Estructura en Dokploy

```
parqueaderos-api/
├── api          (este repo, imagen ghcr.io/jbeleno/parqueaderos-api:latest)
├── ocr          (sidecar Python, imagen ghcr.io/jbeleno/parqueaderos-ocr:latest)
└── db           (PostgreSQL 16 + PostGIS 3.4)
```

### Variables de entorno en Dokploy

| Variable | Default | Obligatoria | Descripción |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://parqueadero-db-f3zhnv-db-1:5432/parqueaderos` | ✅ | JDBC URL hacia el contenedor de Postgres en la misma red |
| `DB_USERNAME` | — | ✅ | Usuario de Postgres |
| `DB_PASSWORD` | — | ✅ | Password de Postgres |
| `JPA_DDL_AUTO` | `update` | ❌ | Estrategia de Hibernate (`update`/`validate`/`none`) |
| `JWT_SECRET` | — | ✅ | Secreto para firmar JWT (`openssl rand -base64 48`) |
| `JWT_ACCESS_EXP` | `3600000` | ❌ | TTL access token (ms) — default 1h |
| `JWT_REFRESH_EXP` | `604800000` | ❌ | TTL refresh token (ms) — default 7d |
| `RESEND_API_KEY` | — | ✅ | API key de Resend para envío de emails |
| `RESEND_FROM` | `onboarding@resend.dev` | ❌ | Remitente verificado |
| `MAIL_HOST` | `smtp.gmail.com` | ❌ | SMTP fallback (si no usas Resend) |
| `MAIL_PORT` | `587` | ❌ | Puerto SMTP |
| `MAIL_USERNAME` | — | ❌ | Usuario SMTP |
| `MAIL_PASSWORD` | — | ❌ | Password SMTP |
| `APP_IMAGES_DIR` | `/app/images` | ❌ | Path interno donde guarda imágenes de cámaras |
| `OCR_ENABLED` | `true` | ❌ | Activa el cliente OCR |
| `OCR_URL` | `http://ocr:8001` | ❌ | URL del sidecar Python en la misma red |
| `OCR_TIMEOUT_MS` | `10000` | ❌ | Timeout de llamada al sidecar |
| `PIN_EXPIRATION` | `15` | ❌ | Minutos de validez del PIN de email |
| `PORT` | `8080` | ❌ | Puerto interno |

### Conectarse a la BD de producción

```bash
# En Dokploy: abrir terminal del contenedor de Postgres
docker exec -it parqueadero-db-f3zhnv-db-1 psql -U $DB_USERNAME -d parqueaderos

# Verificar las nuevas tablas v49
parqueaderos=# \dt
parqueaderos=# SELECT COUNT(*) FROM accion_auditable;       -- debe ser 14
parqueaderos=# SELECT COUNT(*) FROM tipo_documento;          -- debe ser 11
parqueaderos=# SELECT COUNT(*) FROM empresa_config;          -- 26 × N empresas
parqueaderos=# SELECT COUNT(*) FROM reporte_definicion;      -- 6 globales
```

---

## 🏗️ Estructura del proyecto

```
com.usco.parqueaderos_api/
├── auth/         # Login, registro, JWT, PIN, refresh, UsuarioParqueadero (RBAC)
├── user/         # Usuario, Persona, UsuarioRol
├── parking/      # Empresa, Parqueadero, Nivel, Seccion, SubSeccion, PuntoParqueo, Camara
├── ticket/       # Entrada/salida, vehículo abandonado, OCR auto
├── reservation/  # Reservas
├── tariff/       # Tarifas, franjas, calculadora Modelo B + IVA
├── billing/      # Facturas, Pagos, Recibos, LinksPago, ResolucionDian, Backfill
├── vehicle/      # Vehículos (soft-delete)
├── ocr/          # Cliente HTTP al sidecar Python + listener
├── notification/ # Listeners async + WebSocket STOMP
├── subscription/ # Suscripcion (MENSUAL/PASE_DIA/ABONO_PREPAGO) + MovimientoSaldo
├── convenio/     # Convenios + ValidacionCompra
├── caja/         # Apertura/cierre + MovimientoCaja + arqueo
├── audit/        # AuditLog append-only + @Auditable AOP + accion_auditable + nivel_audit_log (v49)
├── report/       # CierreDia + reportes + ReporteUniversal + reporte_definicion (v49)
├── device/       # Sensores, cámaras, barreras (IoT)
├── catalog/      # Estado, Rol, TipoVehiculo, TipoParqueadero, TipoPuntoParqueo, TipoDispositivo
│   ├── global/   #   v49: tipo_documento, genero, moneda, zona_horaria, unidad_tarifa, regimen_tributario
│   └── empresa/  #   v49: estados/tipos por empresa (10 catálogos)
├── config/       # v49: empresa_config (key-value) + empresa_validacion_campo
├── location/     # País > Departamento > Ciudad
└── common/       # CORS, excepciones, health, ApiResponse, BaseEntity (v49)
```

---

## 🧰 Reglas críticas (CLAUDE.md)

### Nunca devolver entidades JPA con lazy desde controllers
Toda entidad con `@ManyToOne(LAZY)` debe mapearse a DTO dentro del `@Transactional` del servicio. Si no, Jackson falla con `ByteBuddyInterceptor`. Patrón:

```java
// En el Service (dentro de @Transactional)
private XxxDTO toDTO(Xxx entity) { ... }

// En el Controller
public ResponseEntity<ApiResponse<XxxDTO>> get(...) { ... }
```

### Soft-delete uniforme (v49)
Toda entidad de negocio expone `archivado_en` + `archivado_por_usuario_id`. El endpoint `PATCH /{id}/archivar` con `X-Motivo-Operacion` registra quién y cuándo. No hay `DELETE` físico salvo para `SUPER_ADMIN` con auditoría obligatoria.

### Seed data idempotente
`data.sql` con `ON CONFLICT DO NOTHING` o `INSERT … WHERE NOT EXISTS`. Contiene: 3 estados, 5 roles, 3 tipos vehículo, 3 tipos parqueadero, 3 tipos punto, 3 tipos dispositivo, 1 país (Colombia), 5 departamentos, 10 ciudades, 14 acciones auditables, 4 niveles audit, 11 tipos documento, 4 géneros, 5 monedas, 7 zonas horarias, 6 unidades tarifa, 5 regímenes tributarios, 26 settings empresa × N empresas, 20 reglas validación × N empresas, 6 reportes globales.

### Spring `ScriptUtils` no entiende `DO $$ ... $$;`
Para CHECK constraints idempotentes usar `ALTER TABLE … DROP CONSTRAINT IF EXISTS …; ALTER TABLE … ADD CONSTRAINT …` (no `DO BEGIN … END;` con dollar quoting).

---

## 📚 Endpoints destacados (v49)

```
# Catálogos globales (lectura libre autenticada)
GET /api/catalogos/tipos-documento
GET /api/catalogos/generos
GET /api/catalogos/monedas
GET /api/catalogos/zonas-horarias
GET /api/catalogos/unidades-tarifa
GET /api/catalogos/regimenes-tributarios

# Catálogos por empresa (scope automático via JWT)
GET /api/catalogos/empresa/metodos-pago
GET /api/catalogos/empresa/estados-ticket
GET /api/catalogos/empresa/estados-factura
GET /api/catalogos/empresa/estados-pago
GET /api/catalogos/empresa/estados-suscripcion
GET /api/catalogos/empresa/estados-caja
GET /api/catalogos/empresa/tipos-movimiento-caja
GET /api/catalogos/empresa/tipos-movimiento-saldo
GET /api/catalogos/empresa/tipos-descuento-convenio
GET /api/catalogos/empresa/origenes-factura

# Configuración por empresa (ADMIN)
GET /api/empresa-config
PUT /api/empresa-config

# Validaciones por empresa (ADMIN)
GET /api/empresa-validaciones
GET /api/empresa-validaciones/{entidad}
PUT /api/empresa-validaciones

# Reportes parametrizables
GET  /api/reportes-parametrizables
POST /api/reportes-parametrizables/{clave}/ejecutar
GET  /api/reportes-historial
```

Resto en [`ENDPOINTS.md`](./ENDPOINTS.md) y Swagger UI.

---

## 🧪 Desarrollo local

### Requisitos
- Java 21
- Maven (incluido `mvnw`)
- Docker + Docker Compose (para BD)

### Clonar + setup

```bash
git clone https://github.com/jbeleno/parqueadero-backend.git
cd parqueadero-backend
cp .env.example .env
# editar .env con tus valores reales
```

### Levantar la BD local (sin la API)

```bash
docker compose up -d db
```

### Levantar TODO local (BD + API)

```bash
docker compose up --build
```

### Solo la API (BD ya corriendo)

```bash
./mvnw spring-boot:run
# o desde el JAR:
./mvnw clean package -DskipTests
java -jar target/parqueaderos-api-0.0.1-SNAPSHOT.jar
```

### Tests

```bash
./mvnw test                     # full suite (139 tests)
./mvnw test -Dtest='TicketServiceTest'   # un solo test
```

### Acceso local

| URL | Descripción |
|---|---|
| `http://localhost:8080` | API base |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/api/health` | Health check |

---

## 🧱 Refactor v49 (Mayo 2026)

Refactor integral que cubre **11 de 13 fases del plan**, **sin breaking changes**:

| Fase | Aporte |
|---|---|
| Sprint A | Snapshots de historicidad en `ticket`/`factura`/`pago` |
| 0 | `BaseEntity` con `fecha_creacion`/`fecha_actualizacion` en 37 entities |
| 1 | 6 catálogos globales (tipo_documento, genero, moneda, etc.) |
| 2 | 10 catálogos por empresa (estados, tipos, métodos de pago) |
| 3 | `empresa_config` key-value + service con cache (26 settings seed) |
| 4 | `empresa_validacion_campo` + service para reglas editables (20 seed) |
| 5 | 9 entities pobres enriquecidas (~45 columnas) |
| 6 | Catálogos legacy con `codigo`/`color_hex`/`icono`/`orden_display` |
| 8 | Reportes parametrizables (6 reportes seed con SQL template) |
| 9 | Auditoría enriquecida (`accion_auditable` + `nivel_audit_log`) |
| 10 | Soft-delete uniforme (`archivado_en` + actor en 12 tablas) |

Detalle en [`REFACTOR_v49.md`](./REFACTOR_v49.md).

---

## 🐛 Troubleshooting

### `PSQLException: FATAL: password authentication failed`
Revisar `DB_PASSWORD` en `.env` o variables de Dokploy.

### `Bad Gateway` desde Traefik
El container no arrancó. Revisar logs en Dokploy → buscar `ApplicationContext` o `Caused by`. Una causa frecuente: `data.sql` con sintaxis no compatible con Spring `ScriptUtils` (ej. `DO $$` blocks).

### `Extension postgis is not available`
La imagen `postgis/postgis:16-3.4-alpine` ya la trae. Para BD externa: `CREATE EXTENSION postgis;` como superuser.

### `column "fecha_creacion" of relation "X" contains null values` al arrancar

Pasó al deployar v49 contra una BD con datos pre-v49. Hibernate
(`ddl-auto=update`) intenta hacer `ALTER COLUMN SET NOT NULL` sobre una
columna recién creada que tiene filas viejas con NULL. **Fix aplicado:**
`BaseEntity.fechaCreacion` ahora es `nullable=true` en JPA — la
consistencia se garantiza via `@PrePersist` a nivel de aplicación. Los
registros pre-v49 quedan con NULL hasta que se reescriban.

### Tests fallan con `Unable to find a @SpringBootConfiguration`
Asegurar que estás en la raíz del proyecto. Usa `./mvnw test` (no `mvn test` global).

### Mixed Content del front HTTPS → backend HTTP
Configurar Dokploy con Traefik + certificado (Let's Encrypt o self-signed) para que la API responda en HTTPS.

---

## 📄 Licencia

Proyecto académico — uso educativo (Universidad Surcolombiana, USCO).

---

**Desarrollado durante pasantías universitarias — Universidad Surcolombiana (USCO), 2026.**
