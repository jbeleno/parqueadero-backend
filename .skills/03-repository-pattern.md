# Skill 03: Patrón de Repository

## Regla
Todos los repositories extienden `JpaRepository<Entidad, Long>`. Se pueden agregar query methods personalizados.

---

## Patrón base

```java
package com.usco.parqueaderos_api.{modulo}.repository;

import com.usco.parqueaderos_api.{modulo}.entity.{Entidad};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {Entidad}Repository extends JpaRepository<{Entidad}, Long> {
}
```

---

## Query methods útiles a agregar

### UsuarioRepository
```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}
```

### VehiculoRepository
```java
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<Vehiculo> findByPersonaId(Long personaId);
}
```

### TicketRepository
```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByParqueaderoId(Long parqueaderoId);
    List<Ticket> findByVehiculoId(Long vehiculoId);
    List<Ticket> findByEstado(String estado);
    Optional<Ticket> findByPuntoParqueoIdAndEstado(Long puntoParqueoId, String estado);
    List<Ticket> findByParqueaderoIdAndEstado(Long parqueaderoId, String estado);
}
```

### ReservaRepository
```java
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuarioId(Long usuarioId);
    List<Reserva> findByParqueaderoId(Long parqueaderoId);
    List<Reserva> findByEstado(String estado);
    List<Reserva> findByParqueaderoIdAndEstado(Long parqueaderoId, String estado);
}
```

### TarifaRepository
```java
@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    List<Tarifa> findByParqueaderoId(Long parqueaderoId);
    List<Tarifa> findByParqueaderoIdAndTipoVehiculoId(Long parqueaderoId, Long tipoVehiculoId);
}
```

### PuntoParqueoRepository
```java
@Repository
public interface PuntoParqueoRepository extends JpaRepository<PuntoParqueo, Long> {
    List<PuntoParqueo> findBySubSeccionId(Long subSeccionId);
    List<PuntoParqueo> findByEstadoId(Long estadoId);
}
```

### NivelRepository
```java
@Repository
public interface NivelRepository extends JpaRepository<Nivel, Long> {
    List<Nivel> findByParqueaderoId(Long parqueaderoId);
}
```

### SeccionRepository
```java
@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findByNivelId(Long nivelId);
    List<Seccion> findByParqueaderoId(Long parqueaderoId);
}
```

### SubSeccionRepository
```java
@Repository
public interface SubSeccionRepository extends JpaRepository<SubSeccion, Long> {
    List<SubSeccion> findBySeccionId(Long seccionId);
}
```

### FacturaRepository
```java
@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    List<Factura> findByParqueaderoId(Long parqueaderoId);
    Optional<Factura> findByTicketId(Long ticketId);
    List<Factura> findByEstado(String estado);
}
```

### PagoRepository
```java
@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByFacturaId(Long facturaId);
}
```

### DispositivoRepository
```java
@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    List<Dispositivo> findByParqueaderoId(Long parqueaderoId);
}
```

### UsuarioRolRepository (NUEVO — no existe actualmente)
```java
package com.usco.parqueaderos_api.user.repository;

import com.usco.parqueaderos_api.user.entity.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    List<UsuarioRol> findByUsuarioId(Long usuarioId);
}
```

---

## Repositorios que no cambian (solo mover de paquete)

Estos repositories se quedan igual, solo cambiar `package`:

- `EstadoRepository` → `catalog.repository`
- `TipoParqueaderoRepository` → `catalog.repository`
- `TipoPuntoParqueoRepository` → `catalog.repository`
- `TipoVehiculoRepository` → `catalog.repository`
- `TipoDispositivoRepository` → `catalog.repository`
- `RolRepository` → `catalog.repository`
- `PaisRepository` → `location.repository`
- `DepartamentoRepository` → `location.repository`
- `CiudadRepository` → `location.repository`
- `EmpresaRepository` → `parking.repository`
- `PersonaRepository` → `user.repository`
- `DispositivoParqueoRepository` → `device.repository` (NO EXISTE AÚN — CREAR)

### DispositivoParqueoRepository (NUEVO)
```java
package com.usco.parqueaderos_api.device.repository;

import com.usco.parqueaderos_api.device.entity.DispositivoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispositivoParqueoRepository extends JpaRepository<DispositivoParqueo, Long> {
    List<DispositivoParqueo> findByDispositivoId(Long dispositivoId);
    List<DispositivoParqueo> findByPuntoParqueoId(Long puntoParqueoId);
}
```
