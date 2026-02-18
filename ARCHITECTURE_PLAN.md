# Plan de Arquitectura — Parqueaderos API

## Instrucciones para el modelo programador

Este documento describe EXACTAMENTE cómo reestructurar el proyecto. Sigue cada fase en orden.
Lee los archivos en `.skills/` para ver los patrones de código exactos que debes usar.

---

## Contexto del Proyecto

- **Framework**: Spring Boot 3.5.10, Java 21
- **Base de datos**: PostgreSQL 17 + PostGIS 3.6 (AWS RDS)
- **Build**: Maven
- **Librerías**: Lombok, Hibernate Spatial, Bean Validation
- **Propósito**: API REST + WebSocket para sistema de gestión de parqueaderos (móvil + web)

---

## FASE 1: Reestructurar Paquetes (Package-by-Feature)

### Estado actual (NO deseado)
```
com.usco.parqueaderos_api/
├── controller/    ← 25 controllers mezclados
├── dto/           ← 13 DTOs
├── entity/        ← 26 entidades
├── repository/    ← 24 repositories
└── service/       ← Solo 2 servicios
```

### Estado objetivo (DESEADO)
```
com.usco.parqueaderos_api/
├── ParqueaderosApiApplication.java
│
├── common/
│   ├── config/
│   │   ├── CorsConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── JacksonConfig.java
│   ├── exception/
│   │   ├── ResourceNotFoundException.java
│   │   ├── BusinessException.java
│   │   ├── DuplicateResourceException.java
│   │   └── GlobalExceptionHandler.java
│   ├── dto/
│   │   └── ApiResponse.java
│   └── event/
│       ├── TicketClosedEvent.java
│       ├── ReservationCreatedEvent.java
│       └── SpotStatusChangedEvent.java
│
├── catalog/
│   ├── entity/
│   │   ├── Estado.java
│   │   ├── TipoParqueadero.java
│   │   ├── TipoPuntoParqueo.java
│   │   ├── TipoVehiculo.java
│   │   ├── TipoDispositivo.java
│   │   └── Rol.java
│   ├── repository/
│   │   ├── EstadoRepository.java
│   │   ├── TipoParqueaderoRepository.java
│   │   ├── TipoPuntoParqueoRepository.java
│   │   ├── TipoVehiculoRepository.java
│   │   ├── TipoDispositivoRepository.java
│   │   └── RolRepository.java
│   ├── service/
│   │   └── CatalogService.java
│   └── controller/
│       └── CatalogController.java
│
├── location/
│   ├── entity/
│   │   ├── Pais.java
│   │   ├── Departamento.java
│   │   └── Ciudad.java
│   ├── repository/
│   │   ├── PaisRepository.java
│   │   ├── DepartamentoRepository.java
│   │   └── CiudadRepository.java
│   ├── service/
│   │   └── LocationService.java
│   └── controller/
│       └── LocationController.java
│
├── parking/
│   ├── entity/
│   │   ├── Empresa.java
│   │   ├── Parqueadero.java
│   │   ├── Nivel.java
│   │   ├── Seccion.java
│   │   ├── SubSeccion.java
│   │   └── PuntoParqueo.java
│   ├── dto/
│   │   ├── ParqueaderoDTO.java
│   │   ├── NivelDTO.java
│   │   ├── SeccionDTO.java
│   │   ├── SubSeccionDTO.java
│   │   └── PuntoParqueoDTO.java
│   ├── repository/
│   │   ├── EmpresaRepository.java
│   │   ├── ParqueaderoRepository.java
│   │   ├── NivelRepository.java
│   │   ├── SeccionRepository.java
│   │   ├── SubSeccionRepository.java
│   │   └── PuntoParqueoRepository.java
│   ├── service/
│   │   ├── EmpresaService.java
│   │   ├── ParqueaderoService.java
│   │   ├── NivelService.java
│   │   ├── SeccionService.java
│   │   ├── SubSeccionService.java
│   │   └── PuntoParqueoService.java
│   ├── controller/
│   │   ├── EmpresaController.java
│   │   ├── ParqueaderoController.java
│   │   ├── NivelController.java
│   │   ├── SeccionController.java
│   │   ├── SubSeccionController.java
│   │   └── PuntoParqueoController.java
│   └── ws/
│       └── ParkingWebSocketHandler.java
│
├── user/
│   ├── entity/
│   │   ├── Persona.java
│   │   ├── Usuario.java
│   │   └── UsuarioRol.java
│   ├── dto/
│   │   └── UsuarioDTO.java
│   ├── repository/
│   │   ├── PersonaRepository.java
│   │   ├── UsuarioRepository.java
│   │   └── UsuarioRolRepository.java
│   ├── service/
│   │   ├── PersonaService.java
│   │   └── UsuarioService.java
│   └── controller/
│       ├── PersonaController.java
│       └── UsuarioController.java
│
├── vehicle/
│   ├── entity/
│   │   └── Vehiculo.java
│   ├── dto/
│   │   └── VehiculoDTO.java
│   ├── repository/
│   │   └── VehiculoRepository.java
│   ├── service/
│   │   └── VehiculoService.java
│   └── controller/
│       └── VehiculoController.java
│
├── ticket/
│   ├── entity/
│   │   └── Ticket.java
│   ├── dto/
│   │   └── TicketDTO.java
│   ├── repository/
│   │   └── TicketRepository.java
│   ├── service/
│   │   └── TicketService.java
│   ├── controller/
│   │   └── TicketController.java
│   └── ws/
│       └── TicketWebSocketHandler.java
│
├── reservation/
│   ├── entity/
│   │   └── Reserva.java
│   ├── dto/
│   │   └── ReservaDTO.java
│   ├── repository/
│   │   └── ReservaRepository.java
│   ├── service/
│   │   └── ReservaService.java
│   └── controller/
│       └── ReservaController.java
│
├── tariff/
│   ├── entity/
│   │   └── Tarifa.java
│   ├── dto/
│   │   └── TarifaDTO.java
│   ├── repository/
│   │   └── TarifaRepository.java
│   ├── service/
│   │   └── TarifaService.java
│   └── controller/
│       └── TarifaController.java
│
├── billing/
│   ├── entity/
│   │   ├── Factura.java
│   │   └── Pago.java
│   ├── dto/
│   │   ├── FacturaDTO.java
│   │   └── PagoDTO.java
│   ├── repository/
│   │   ├── FacturaRepository.java
│   │   └── PagoRepository.java
│   ├── service/
│   │   ├── FacturaService.java
│   │   └── PagoService.java
│   ├── controller/
│   │   ├── FacturaController.java
│   │   └── PagoController.java
│   └── listener/
│       └── BillingEventListener.java
│
├── device/
│   ├── entity/
│   │   ├── Dispositivo.java
│   │   └── DispositivoParqueo.java
│   ├── dto/
│   │   └── DispositivoDTO.java
│   ├── repository/
│   │   ├── DispositivoRepository.java
│   │   └── DispositivoParqueoRepository.java
│   ├── service/
│   │   └── DispositivoService.java
│   └── controller/
│       └── DispositivoController.java
│
├── notification/
│   ├── dto/
│   │   └── NotificationDTO.java
│   ├── service/
│   │   ├── NotificationService.java
│   │   └── WebSocketNotificationService.java
│   └── listener/
│       └── NotificationEventListener.java
│
└── auth/
    ├── config/
    │   └── SecurityConfig.java
    ├── dto/
    │   ├── LoginRequest.java
    │   ├── LoginResponse.java
    │   └── RegisterRequest.java
    ├── service/
    │   ├── AuthService.java
    │   └── JwtService.java
    ├── filter/
    │   └── JwtAuthenticationFilter.java
    └── controller/
        └── AuthController.java
```

### Instrucciones para mover archivos

**IMPORTANTE**: Al mover archivos, actualiza SIEMPRE el `package` en la primera línea del archivo Java y los `import` en los archivos que los refferencian.

**Orden de migración** (para evitar errores de compilación):
1. Crear paquetes vacíos primero
2. Mover entidades (no tienen dependencias internas complejas)
3. Mover repositorios (dependen solo de entidades)
4. Mover DTOs
5. Crear/mover servicios (dependen de repositorios + entidades)
6. Mover/reescribir controllers (dependen de servicios)

---

## FASE 2: Excepciones Tipadas

### Qué hacer
1. Eliminar el `GlobalExceptionHandler.java` actual
2. Crear las excepciones del paquete `common/exception/`
3. Crear el nuevo `GlobalExceptionHandler.java`

### Archivos a crear
Ver `.skills/06-exception-handling.md` para el código exacto.

| Clase | Uso |
|-------|-----|
| `ResourceNotFoundException` | Cuando un `findById` no encuentra resultado → HTTP 404 |
| `BusinessException` | Errores de lógica de negocio → HTTP 422 |
| `DuplicateResourceException` | Cuando se intenta crear un recurso duplicado → HTTP 409 |
| `GlobalExceptionHandler` | Captura todas las excepciones y devuelve `ApiResponse` |
| `ApiResponse<T>` | Envolvente estándar para TODAS las respuestas de la API |

---

## FASE 3: Capa de Servicio Completa

### Problema actual
23 de 25 controllers inyectan `Repository` directamente. Solo `ParqueaderoController` y `TarifaController` usan un `Service`.

### Qué hacer
Crear un `*Service.java` para CADA módulo siguiendo el patrón de `.skills/04-service-pattern.md`.

### Lista de servicios a crear

| Módulo | Servicio | Repository que usa | DTO |
|--------|----------|--------------------|-----|
| catalog | `CatalogService` | `EstadoRepository`, `TipoParqueaderoRepo`, `TipoPuntoParqueoRepo`, `TipoVehiculoRepo`, `TipoDispositivoRepo`, `RolRepo` | No necesita (entidades simples) |
| location | `LocationService` | `PaisRepository`, `DepartamentoRepo`, `CiudadRepo` | No necesita (entidades simples) |
| parking | `EmpresaService` | `EmpresaRepository` | No necesita |
| parking | `ParqueaderoService` | `ParqueaderoRepository`, etc. | `ParqueaderoDTO` (ya existe, corregir) |
| parking | `NivelService` | `NivelRepository` | `NivelDTO` (ya existe) |
| parking | `SeccionService` | `SeccionRepository` | `SeccionDTO` (ya existe) |
| parking | `SubSeccionService` | `SubSeccionRepository` | `SubSeccionDTO` (ya existe) |
| parking | `PuntoParqueoService` | `PuntoParqueoRepository` | `PuntoParqueoDTO` (ya existe) |
| user | `PersonaService` | `PersonaRepository` | No necesita |
| user | `UsuarioService` | `UsuarioRepository` | `UsuarioDTO` (ya existe) |
| vehicle | `VehiculoService` | `VehiculoRepository` | `VehiculoDTO` (ya existe) |
| ticket | `TicketService` | `TicketRepository` | `TicketDTO` (ya existe) |
| reservation | `ReservaService` | `ReservaRepository` | `ReservaDTO` (ya existe) |
| tariff | `TarifaService` | `TarifaRepository` | `TarifaDTO` (ya existe, corregir) |
| billing | `FacturaService` | `FacturaRepository` | `FacturaDTO` (ya existe) |
| billing | `PagoService` | `PagoRepository` | `PagoDTO` (ya existe) |
| device | `DispositivoService` | `DispositivoRepository` | `DispositivoDTO` (ya existe) |

### Bug crítico a corregir
En `ParqueaderoService.convertToEntity()` y `TarifaService.convertToEntity()`, las relaciones FK NO se cargan. La solución:

```java
// INCORRECTO (actual):
private Parqueadero convertToEntity(ParqueaderoDTO dto) {
    Parqueadero entity = new Parqueadero();
    entity.setNombre(dto.getNombre());
    // ... FKs NO se setean
    return entity;
}

// CORRECTO (nuevo):
private Parqueadero convertToEntity(ParqueaderoDTO dto) {
    Parqueadero entity = new Parqueadero();
    entity.setNombre(dto.getNombre());
    // ... campos propios
    
    // CARGAR FKs desde repositorios:
    if (dto.getCiudadId() != null) {
        entity.setCiudad(ciudadRepository.findById(dto.getCiudadId())
            .orElseThrow(() -> new ResourceNotFoundException("Ciudad", dto.getCiudadId())));
    }
    if (dto.getEmpresaId() != null) {
        entity.setEmpresa(empresaRepository.findById(dto.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa", dto.getEmpresaId())));
    }
    // ... etc para cada FK
    return entity;
}
```

---

## FASE 4: Reescribir Controllers

### Qué hacer
Todos los controllers deben seguir el patrón de `.skills/05-controller-pattern.md`:
- Inyectar `*Service`, NUNCA `*Repository`
- Usar `@Valid` en `@RequestBody`
- Usar `ApiResponse<T>` como wrapper de respuesta
- Usar `HttpStatus.CREATED` (201) en POST
- Usar `HttpStatus.NO_CONTENT` (204) en DELETE

### Patrón de controller nuevo
Ver `.skills/05-controller-pattern.md` para el código completo.

---

## FASE 5: Activar Validación (Bean Validation)

### Qué hacer
Agregar anotaciones de validación a TODOS los DTOs que reciben input del usuario.
Ver `.skills/07-validation-pattern.md` para el código exacto.

### Ejemplo
```java
@Data
public class ParqueaderoDTO {
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;
    
    @Size(max = 300)
    private String direccion;
    
    @NotNull(message = "La ciudad es obligatoria")
    private Long ciudadId;
    // ... etc
}
```

---

## FASE 6: Dependencias Nuevas en pom.xml

### Agregar al `<dependencies>` del pom.xml

```xml
<!-- WebSocket + STOMP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- OpenAPI / Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

---

## FASE 7: WebSocket (STOMP)

### Qué hacer
1. Crear `WebSocketConfig.java` en `common/config/`
2. Crear handlers de WebSocket en los módulos que lo necesitan
3. Integrar con Spring Events

### Canales WebSocket

| Canal STOMP | Descripción | Quién publica |
|-------------|-------------|---------------|
| `/topic/parking/{id}/spots` | Estado de puestos en tiempo real (libre/ocupado) | `TicketService`, `ReservaService` |
| `/topic/parking/{id}/tickets` | Entradas/salidas en tiempo real | `TicketService` |
| `/user/{userId}/notifications` | Notificaciones personales | `NotificationService` |
| `/topic/parking/{id}/devices` | Alertas de sensores | `DispositivoService` |

Ver `.skills/08-websocket-pattern.md` para el código completo.

---

## FASE 8: Seguridad (JWT + Spring Security)

### Qué hacer
1. Crear tabla en DB (ya existe `usuario` con `password_hash`)
2. Crear `SecurityConfig.java`, `JwtService.java`, `JwtAuthenticationFilter.java`
3. Crear `AuthController.java` con endpoints `/api/auth/login` y `/api/auth/register`
4. Proteger endpoints (los de catálogo y health son públicos)

### Endpoints públicos (sin JWT)
- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/paises/**`, `GET /api/departamentos/**`, `GET /api/ciudades/**`
- `GET /api/estados/**`
- WebSocket handshake `/ws/**`

### Endpoints protegidos (requieren JWT)
- Todos los demás

Ver `.skills/09-security-jwt-pattern.md` para el código completo.

---

## FASE 9: Sistema de Eventos

### Qué hacer
1. Crear eventos en `common/event/`
2. Publicar eventos desde los servicios
3. Crear listeners en los módulos que reaccionan

### Flujo de eventos

```
TicketService.closeTicket()
    → publica TicketClosedEvent
        → BillingEventListener: genera Factura automática
        → NotificationEventListener: envía notificación al usuario
        → ParkingWebSocketHandler: actualiza estado del puesto en el mapa

ReservaService.createReserva()
    → publica ReservationCreatedEvent
        → NotificationEventListener: confirma reserva al usuario
        → ParkingWebSocketHandler: marca puesto como reservado

TicketService.createTicket() / DispositivoService.reportSpotChange()
    → publica SpotStatusChangedEvent
        → ParkingWebSocketHandler: broadcast estado del puesto
```

Ver `.skills/10-events-pattern.md` para el código completo.

---

## FASE 10: Notificaciones

### Qué hacer
1. Crear `NotificationService` que centraliza el envío
2. Canal 1: WebSocket (in-app, tiempo real)
3. Canal 2: Firebase Cloud Messaging (push móvil) — se puede hacer después

Ver `.skills/11-notification-pattern.md` para el código completo.

---

## FASE 11: Configuración del application.yaml

### Estado actual (PROBLEMAS)
- Credenciales en texto plano
- Sin configuración de WebSocket
- Sin perfiles de entorno

### application.yaml nuevo

```yaml
spring:
  application:
    name: parqueaderos-api
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/parqueaderos}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:update}
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

server:
  port: ${SERVER_PORT:8080}

# JWT
app:
  jwt:
    secret: ${JWT_SECRET:clave-secreta-desarrollo-cambiar-en-produccion-min-256-bits-xxxxx}
    expiration-ms: ${JWT_EXPIRATION:86400000}
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION:604800000}

# Swagger
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## Orden de ejecución (RESUMEN)

| # | Fase | Descripción | Skill de referencia |
|---|------|-------------|---------------------|
| 1 | Dependencias | Agregar dependencias al pom.xml | Fase 6 de este documento |
| 2 | Excepciones | Crear excepciones tipadas + ApiResponse | `.skills/06-exception-handling.md` |
| 3 | Paquetes | Mover archivos a estructura package-by-feature | Fase 1 de este documento |
| 4 | Servicios | Crear servicios para todos los módulos | `.skills/04-service-pattern.md` |
| 5 | Controllers | Reescribir controllers con nuevo patrón | `.skills/05-controller-pattern.md` |
| 6 | Validación | Agregar @Valid y anotaciones a DTOs | `.skills/07-validation-pattern.md` |
| 7 | application.yaml | Actualizar config con variables de entorno | Fase 11 de este documento |
| 8 | WebSocket | Config STOMP + handlers | `.skills/08-websocket-pattern.md` |
| 9 | Seguridad | JWT + Spring Security | `.skills/09-security-jwt-pattern.md` |
| 10 | Eventos | Sistema de eventos internos | `.skills/10-events-pattern.md` |
| 11 | Notificaciones | Servicio de notificaciones | `.skills/11-notification-pattern.md` |

---

## Reglas inquebrantables

1. **NUNCA** inyectar un `Repository` en un `Controller`. Siempre pasar por `Service`.
2. **SIEMPRE** usar `@Valid` en `@RequestBody`.
3. **SIEMPRE** lanzar `ResourceNotFoundException` cuando `findById` retorna vacío.
4. **SIEMPRE** cargar las FKs desde los repositorios en `convertToEntity()`.
5. **SIEMPRE** usar `@Transactional` en métodos de servicio. `(readOnly = true)` para lecturas.
6. **SIEMPRE** devolver `ApiResponse<T>` desde los controllers.
7. **NUNCA** poner credenciales hardcodeadas en archivos de configuración.
8. **SIEMPRE** usar `@CrossOrigin` centralizado en `CorsConfig.java`, no en cada controller.
9. **SIEMPRE** retornar `201 CREATED` en POST, `204 NO CONTENT` en DELETE.
10. **NUNCA** exponer entidades JPA directamente en controllers que tienen relaciones complejas. Usar DTOs.
