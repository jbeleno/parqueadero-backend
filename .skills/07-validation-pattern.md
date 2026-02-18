# Skill 07: Patrón de Validación (Bean Validation)

## Regla
La dependencia `spring-boot-starter-validation` ya existe en el pom.xml. Solo falta activarla.

---

## Activar validación

1. Poner `@Valid` antes de `@RequestBody` en los controllers
2. Agregar anotaciones de validación en los DTOs y entidades que reciben input

---

## Anotaciones disponibles (Jakarta Validation)

```java
import jakarta.validation.constraints.*;
```

| Anotación | Uso | Ejemplo |
|-----------|-----|---------|
| `@NotNull` | Campo no puede ser null | `@NotNull private Long ciudadId;` |
| `@NotBlank` | String no null, no vacío, no solo espacios | `@NotBlank private String nombre;` |
| `@NotEmpty` | Colección/String no vacía | `@NotEmpty private List<Long> ids;` |
| `@Size(min, max)` | Longitud de String o colección | `@Size(max = 200) private String nombre;` |
| `@Min(value)` | Número mínimo | `@Min(0) private Integer cantidad;` |
| `@Max(value)` | Número máximo | `@Max(100) private Integer porcentaje;` |
| `@Positive` | Número > 0 | `@Positive private Double valor;` |
| `@PositiveOrZero` | Número >= 0 | `@PositiveOrZero private Integer stock;` |
| `@Email` | Formato email válido | `@Email private String correo;` |
| `@Pattern(regexp)` | Regex personalizado | `@Pattern(regexp = "^[A-Z]{3}\\d{3}$") private String placa;` |
| `@Past` | Fecha en el pasado | `@Past private LocalDate fechaNacimiento;` |
| `@Future` | Fecha en el futuro | `@Future private LocalDateTime fechaReserva;` |

---

## Validaciones en entidades de catálogo (sin DTO)

Para entidades simples que se reciben directamente en `@RequestBody`:

### Estado.java
```java
@Entity
@Table(name = "estado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre del estado es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
```

### Pais.java
```java
@Entity
@Table(name = "pais")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pais {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre del país es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Size(max = 10)
    @Column(length = 10)
    private String acronimo;
    
    @Size(max = 10)
    @Column(name = "identificador_internacional", length = 10)
    private String identificadorInternacional;
}
```

### Persona.java
```java
@Entity
@Table(name = "persona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Column(nullable = false, length = 100)
    private String apellido;
    
    @Size(max = 20)
    @Column(length = 20)
    private String telefono;
    
    @Size(max = 50)
    @Column(name = "tipo_documento", length = 50)
    private String tipoDocumento;
    
    @Size(max = 50)
    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;
}
```

---

## Validaciones en DTOs (para entidades con FKs)

La validación completa de los DTOs está en `02-dto-pattern.md`. Resumen rápido:

```java
@Data
public class TicketDTO {
    private Long id;
    
    // No validar fechaHoraEntrada — se auto-asigna en el service
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
    
    private String estado;
    private Double montoCalculado;
}
```

---

## En el Controller

```java
// ANTES (sin validación):
@PostMapping
public ResponseEntity<Parqueadero> create(@RequestBody ParqueaderoDTO dto) { ... }

// DESPUÉS (con validación):
@PostMapping
public ResponseEntity<ApiResponse<ParqueaderoDTO>> create(@Valid @RequestBody ParqueaderoDTO dto) { ... }
//                                                          ^^^^^
//                                                          AGREGAR @Valid
```

---

## Respuesta automática de error de validación

Cuando `@Valid` falla, Spring lanza `MethodArgumentNotValidException`, que el `GlobalExceptionHandler` captura y devuelve:

```json
{
    "success": false,
    "message": "Error de validación",
    "data": {
        "nombre": "El nombre es obligatorio",
        "ciudadId": "La ciudad es obligatoria",
        "valor": "El valor debe ser positivo"
    },
    "timestamp": "2026-02-17T10:30:00"
}
```

No necesitas hacer nada más — el handler lo maneja automáticamente.
