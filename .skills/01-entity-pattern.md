# Skill 01: Patrón de Entidad JPA

## Regla
Todas las entidades JPA del proyecto siguen EXACTAMENTE este patrón. NO modificar las entidades existentes a menos que se indique.

---

## Patrón base (entidad simple sin geometría)

```java
package com.usco.parqueaderos_api.{modulo}.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nombre_tabla_snake_case")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NombreEntidad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Campos simples
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    // Relación ManyToOne (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
}
```

---

## Patrón con geometría PostGIS

```java
package com.usco.parqueaderos_api.parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "punto_parqueo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntoParqueo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    // Geometría PostGIS - Polígono
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon poligono;
    
    // Geometría PostGIS - Punto
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point coordenada;
    
    // FKs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_seccion_id", nullable = false)
    private SubSeccion subSeccion;
}
```

---

## Reglas de entidades existentes

### NUNCA cambiar
- El nombre de la tabla (`@Table(name = "...")`)
- El nombre de las columnas de FK (`@JoinColumn(name = "...")`)
- El tipo del `@Id` (siempre `Long` con `GenerationType.IDENTITY`)
- Las relaciones existentes

### Se puede cambiar
- Agregar nuevos campos a una entidad existente
- Agregar nuevas relaciones

---

## Lista completa de entidades y sus paquetes destino

### Módulo `catalog`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Estado` | `estado` | nombre, descripcion | (ninguna) |
| `TipoParqueadero` | `tipo_parqueadero` | nombre, descripcion | estado_id |
| `TipoPuntoParqueo` | `tipo_punto_parqueo` | nombre, descripcion | estado_id |
| `TipoVehiculo` | `tipo_vehiculo` | nombre, descripcion | (ninguna) |
| `TipoDispositivo` | `tipo_dispositivo` | nombre, descripcion | estado_id |
| `Rol` | `rol` | nombre, descripcion | estado_id |

### Módulo `location`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Pais` | `pais` | nombre, acronimo, identificadorInternacional | (ninguna) |
| `Departamento` | `departamento` | nombre, identificadorNacional | pais_id |
| `Ciudad` | `ciudad` | nombre, identificadorDepartamental | departamento_id |

### Módulo `parking`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Empresa` | `empresa` | nombre, descripcion | estado_id |
| `Parqueadero` | `parqueadero` | nombre, direccion, telefono, latitud, longitud, altitud, horaInicio, horaFin, numeroPuntosParqueo, zonaHoraria, tiempoGraciaMinutos, modoCobro | ciudad_id, empresa_id, tipo_parqueadero_id, estado_id |
| `Nivel` | `nivel` | nombre | parqueadero_id, estado_id |
| `Seccion` | `seccion` | nombre, acronimo, descripcion, poligono(Polygon), coordenadaCentro(Point) | parqueadero_id, nivel_id, estado_id |
| `SubSeccion` | `sub_seccion` | nombre, acronimo, descripcion, poligono(Polygon), coordenadaCentro(Point) | seccion_id, estado_id |
| `PuntoParqueo` | `punto_parqueo` | nombre, acronimo, descripcion, poligono(Polygon), coordenada(Point) | sub_seccion_id, tipo_punto_parqueo_id, estado_id |

### Módulo `user`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Persona` | `persona` | nombre, apellido, telefono, tipoDocumento, numeroDocumento | (ninguna) |
| `Usuario` | `usuario` | correo, passwordHash, fechaCreacion | estado_id, persona_id, empresa_id(nullable) |
| `UsuarioRol` | `usuario_rol` | (ninguno) | usuario_id, rol_id |

### Módulo `vehicle`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Vehiculo` | `vehiculo` | placa, color | persona_id, tipo_vehiculo_id |

### Módulo `ticket`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Ticket` | `ticket` | fechaHoraEntrada, fechaHoraSalida(nullable), estado(String), montoCalculado | parqueadero_id, punto_parqueo_id, vehiculo_id, tarifa_id |

### Módulo `reservation`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Reserva` | `reserva` | fechaHoraInicio, fechaHoraFin, estado(String) | parqueadero_id, punto_parqueo_id(nullable), usuario_id, vehiculo_id |

### Módulo `tariff`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Tarifa` | `tarifa` | nombre, valor, unidad, minutosFraccion, fechaInicioVigencia, fechaFinVigencia | parqueadero_id, tipo_vehiculo_id |

### Módulo `billing`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Factura` | `factura` | fechaHora, valorTotal, estado(String) | ticket_id, parqueadero_id, vehiculo_id |
| `Pago` | `pago` | monto, metodo(String), fechaHora, estado(String) | factura_id |

### Módulo `device`
| Entidad | Tabla | Campos propios | FKs |
|---------|-------|---------------|-----|
| `Dispositivo` | `dispositivo` | nombre, urlEndpoint, coordenada(Point) | tipo_dispositivo_id, estado_id, sub_seccion_id(nullable), parqueadero_id |
| `DispositivoParqueo` | `dispositivo_parqueo` | (ninguno) | dispositivo_id, punto_parqueo_id |

---

## Al mover entidades de paquete

Solo cambiar la línea `package`. Ejemplo:

```java
// ANTES:
package com.usco.parqueaderos_api.entity;

// DESPUÉS (para Ciudad):
package com.usco.parqueaderos_api.location.entity;
```

Y actualizar los `import` en TODOS los archivos que referencien esa entidad.
