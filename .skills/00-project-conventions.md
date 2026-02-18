# Skill 00: Convenciones del Proyecto

## Información del proyecto

- **Paquete base**: `com.usco.parqueaderos_api`
- **Java**: 21
- **Spring Boot**: 3.5.10
- **Lombok**: Sí (usar SIEMPRE `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`)
- **Base de datos**: PostgreSQL 17 + PostGIS 3.6
- **Nomenclatura BD**: snake_case (`punto_parqueo`, `tipo_vehiculo`)
- **Nomenclatura Java**: camelCase (`puntoParqueo`, `tipoVehiculo`)

---

## Reglas de nombres

### Paquetes
```
com.usco.parqueaderos_api.{modulo}.{capa}
```
Ejemplos:
- `com.usco.parqueaderos_api.parking.entity`
- `com.usco.parqueaderos_api.ticket.service`
- `com.usco.parqueaderos_api.common.exception`

### Clases
| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| Entidad | Nombre singular en español | `Parqueadero`, `PuntoParqueo` |
| Repository | `{Entidad}Repository` | `ParqueaderoRepository` |
| Service | `{Entidad}Service` | `ParqueaderoService` |
| Controller | `{Entidad}Controller` | `ParqueaderoController` |
| DTO | `{Entidad}DTO` | `ParqueaderoDTO` |
| Excepción | `{Descripcion}Exception` | `ResourceNotFoundException` |
| Evento | `{Accion}Event` | `TicketClosedEvent` |
| Config | `{Funcionalidad}Config` | `WebSocketConfig` |

### URLs de API
- Base: `/api`
- Plural en español: `/api/parqueaderos`, `/api/tickets`, `/api/reservas`
- Usar kebab-case para compuestos: `/api/puntos-parqueo`, `/api/tipos-vehiculo`
- IDs: `/api/parqueaderos/{id}`
- Sub-recursos: `/api/parqueaderos/{parqueaderoId}/niveles`

---

## Imports estándar

### Entidad
```java
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
```

### Service
```java
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

### Controller
```java
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
```

### Repository
```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
```

---

## Módulos del proyecto

| Módulo | Paquete | Entidades contenidas |
|--------|---------|---------------------|
| `common` | `common.config`, `common.exception`, `common.dto`, `common.event` | Sin entidades (transversal) |
| `catalog` | `catalog.*` | Estado, TipoParqueadero, TipoPuntoParqueo, TipoVehiculo, TipoDispositivo, Rol |
| `location` | `location.*` | Pais, Departamento, Ciudad |
| `parking` | `parking.*` | Empresa, Parqueadero, Nivel, Seccion, SubSeccion, PuntoParqueo |
| `user` | `user.*` | Persona, Usuario, UsuarioRol |
| `vehicle` | `vehicle.*` | Vehiculo |
| `ticket` | `ticket.*` | Ticket |
| `reservation` | `reservation.*` | Reserva |
| `tariff` | `tariff.*` | Tarifa |
| `billing` | `billing.*` | Factura, Pago |
| `device` | `device.*` | Dispositivo, DispositivoParqueo |
| `notification` | `notification.*` | Sin entidades (solo servicio) |
| `auth` | `auth.*` | Sin entidades (usa Usuario) |

---

## Estructura de un módulo estándar

```
{modulo}/
├── entity/         ← Entidades JPA
├── dto/            ← DTOs de entrada/salida
├── repository/     ← Interfaces JpaRepository
├── service/        ← Lógica de negocio
├── controller/     ← Endpoints REST
├── ws/             ← WebSocket handlers (solo si aplica)
└── listener/       ← Event listeners (solo si aplica)
```

---

## Dependencias entre módulos (permitidas)

```
common ← todos los módulos pueden usar common
catalog ← parking, user, vehicle, ticket, reservation, device
location ← parking
parking ← ticket, reservation, tariff, billing, device
user ← ticket, reservation, billing, auth, notification
vehicle ← ticket, reservation
tariff ← ticket
ticket ← billing
billing ← (ninguno depende de billing)
device ← (ninguno depende de device)
notification ← (ninguno, solo escucha eventos)
auth ← (ninguno, solo intercepta requests)
```

**NUNCA** crear dependencias circulares. Si dos módulos necesitan comunicarse, usar Spring Events (ver `10-events-pattern.md`).
