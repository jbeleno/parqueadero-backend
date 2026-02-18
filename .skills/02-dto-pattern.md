# Skill 02: Patrón de DTO

## Regla
Los DTOs son objetos planos que se usan para entrada/salida en la API. NUNCA exponer entidades JPA directamente en controllers que tengan relaciones complejas.

---

## Patrón de DTO

```java
package com.usco.parqueaderos_api.{modulo}.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EjemploDTO {
    
    // --- Campos de solo lectura (output) ---
    private Long id;
    
    // --- Campos de entrada con validación ---
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;
    
    @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
    private String direccion;
    
    // --- FK como ID (entrada) + nombre aplanado (salida) ---
    @NotNull(message = "La ciudad es obligatoria")
    private Long ciudadId;
    private String ciudadNombre;  // Solo lectura, se llena desde el Service
    
    private Long estadoId;
    private String estadoNombre;  // Solo lectura
}
```

---

## Convención de campos FK en DTOs

Para cada relación `@ManyToOne` en la entidad, el DTO tiene DOS campos:
1. `{relacion}Id` (Long) — Para entrada: el cliente envía el ID
2. `{relacion}Nombre` (String) — Para salida: el server llena el nombre legible

```java
// En la entidad:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ciudad_id", nullable = false)
private Ciudad ciudad;

// En el DTO:
@NotNull(message = "La ciudad es obligatoria")
private Long ciudadId;        // El cliente envía: {"ciudadId": 5}
private String ciudadNombre;  // El server responde: {"ciudadId": 5, "ciudadNombre": "Neiva"}
```

---

## DTOs que ya existen (reciclar, agregar validación)

### ParqueaderoDTO — paquete destino: `parking.dto`
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParqueaderoDTO {
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;
    
    @Size(max = 300)
    private String direccion;
    
    @Size(max = 20)
    private String telefono;
    
    private Double latitud;
    private Double longitud;
    
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "Formato de hora: HH:mm")
    private String horaInicio;
    
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "Formato de hora: HH:mm")
    private String horaFin;
    
    @Min(0)
    private Integer numeroPuntosParqueo;
    
    @Min(0)
    private Integer tiempoGraciaMinutos;
    
    @Pattern(regexp = "^(POR_HORA|POR_FRACCION|POR_DIA|PLANA)$", message = "Modo de cobro inválido")
    private String modoCobro;
    
    @NotNull(message = "La ciudad es obligatoria")
    private Long ciudadId;
    private String ciudadNombre;
    
    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    private String empresaNombre;
    
    @NotNull(message = "El tipo de parqueadero es obligatorio")
    private Long tipoParqueaderoId;
    private String tipoParqueaderoNombre;
    
    private Long estadoId;
    private String estadoNombre;
}
```

### TarifaDTO — paquete destino: `tariff.dto`
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaDTO {
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;
    
    @NotNull(message = "El valor es obligatorio")
    @Positive(message = "El valor debe ser positivo")
    private Double valor;
    
    @Pattern(regexp = "^(POR_HORA|POR_FRACCION|POR_DIA|PLANA)$")
    private String unidad;
    
    @Min(1)
    private Integer minutosFraccion;
    
    private String fechaInicioVigencia;  // formato: "yyyy-MM-dd"
    private String fechaFinVigencia;
    
    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;
    
    @NotNull(message = "El tipo de vehículo es obligatorio")
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;
}
```

### TicketDTO — paquete destino: `ticket.dto`
```java
@Data
public class TicketDTO {
    private Long id;
    
    private LocalDateTime fechaHoraEntrada;
    private LocalDateTime fechaHoraSalida;
    
    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;
    
    @NotNull(message = "El punto de parqueo es obligatorio")
    private Long puntoParqueoId;
    private String puntoParqueoNombre;
    
    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;
    private String tarifaNombre;
    
    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;
    
    private String estado;  // EN_CURSO, CERRADO, ANULADO
    private Double montoCalculado;
}
```

### ReservaDTO — paquete destino: `reservation.dto`
```java
@Data
public class ReservaDTO {
    private Long id;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaHoraInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaHoraFin;
    
    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;
    private String usuarioNombre;
    
    private Long puntoParqueoId;
    private String puntoParqueoNombre;
    
    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;
    
    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;
    
    private String estado;  // PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA
}
```

### NivelDTO — paquete destino: `parking.dto`
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NivelDTO {
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;
    
    private Long estadoId;
    private String estadoNombre;
}
```

### FacturaDTO — paquete destino: `billing.dto`
```java
@Data
public class FacturaDTO {
    private Long id;
    
    private LocalDateTime fechaHora;
    
    @NotNull(message = "El valor total es obligatorio")
    @Positive
    private Double valorTotal;
    
    @NotNull(message = "El ticket es obligatorio")
    private Long ticketId;
    
    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;
    
    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;
    
    private String estado;  // PENDIENTE, PAGADA, ANULADA
}
```

### PagoDTO — paquete destino: `billing.dto`
```java
@Data
public class PagoDTO {
    private Long id;
    
    private LocalDateTime fechaHora;
    
    @NotNull(message = "El monto es obligatorio")
    @Positive
    private Double monto;
    
    @NotBlank(message = "El método de pago es obligatorio")
    @Pattern(regexp = "^(EFECTIVO|TARJETA|APP)$")
    private String metodo;
    
    @NotNull(message = "La factura es obligatoria")
    private Long facturaId;
    
    private String estado;  // PENDIENTE, COMPLETADO, FALLIDO
}
```

---

## Cuándo usar DTO vs Entidad directa

| Tipo de entidad | Usar DTO | Razón |
|----------------|----------|-------|
| Entidad con FKs complejas (Parqueadero, Ticket, etc.) | **SÍ** | Aplanar relaciones para el JSON |
| Entidad catálogo simple (Estado, TipoVehiculo, Pais, etc.) | **NO** | Son planas, no tienen FKs complejas o solo tienen una FK a Estado |
| Entidades que se envían por WebSocket | **SÍ** | Controlar exactamente qué se serializa |

Para catálogos simples (Estado, Pais, TipoVehiculo, etc.) se pueden usar las entidades directamente en el controller ya que no tienen relaciones profundas que causen problemas de serialización.
