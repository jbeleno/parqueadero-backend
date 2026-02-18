# Skill 06: Manejo de Excepciones

## Archivos a crear

Todos en el paquete `com.usco.parqueaderos_api.common.exception` y `com.usco.parqueaderos_api.common.dto`.

---

## 1. ApiResponse.java (paquete: common.dto)

Envolvente estándar para TODAS las respuestas de la API.

```java
package com.usco.parqueaderos_api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
```

---

## 2. ResourceNotFoundException.java

Lanzar cuando `findById()` o similar no encuentra la entidad. Produce HTTP 404.

```java
package com.usco.parqueaderos_api.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " con ID " + id + " no encontrado");
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

---

## 3. BusinessException.java

Lanzar para errores de lógica de negocio (ej: "no se puede cerrar un ticket que ya está cerrado"). Produce HTTP 422.

```java
package com.usco.parqueaderos_api.common.exception;

public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
}
```

---

## 4. DuplicateResourceException.java

Lanzar cuando se intenta crear un recurso que ya existe (ej: placa duplicada, correo duplicado). Produce HTTP 409.

```java
package com.usco.parqueaderos_api.common.exception;

public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String resourceName, String field, String value) {
        super(resourceName + " con " + field + " '" + value + "' ya existe");
    }
}
```

---

## 5. GlobalExceptionHandler.java (REEMPLAZA el actual)

```java
package com.usco.parqueaderos_api.common.exception;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 404 - No encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    // 422 - Error de negocio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    // 409 - Recurso duplicado
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    // 400 - Errores de validación (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Error de validación");
        response.setData(errors);
        response.setTimestamp(java.time.LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // 500 - Error genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor"));
    }
}
```

---

## Cómo usar las excepciones en Services

```java
// 404 - Entidad no encontrada
parqueaderoRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));

// 409 - Duplicado
if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
    throw new DuplicateResourceException("Usuario", "correo", dto.getCorreo());
}

// 422 - Lógica de negocio
if ("CERRADO".equals(ticket.getEstado())) {
    throw new BusinessException("No se puede cerrar un ticket que ya está cerrado");
}
```

---

## Respuesta JSON de ejemplo

### Éxito (200)
```json
{
    "success": true,
    "data": { "id": 1, "nombre": "Parqueadero Centro", ... },
    "timestamp": "2026-02-17T10:30:00"
}
```

### Error 404
```json
{
    "success": false,
    "message": "Parqueadero con ID 99 no encontrado",
    "timestamp": "2026-02-17T10:30:00"
}
```

### Error 400 (validación)
```json
{
    "success": false,
    "message": "Error de validación",
    "data": {
        "nombre": "El nombre es obligatorio",
        "ciudadId": "La ciudad es obligatoria"
    },
    "timestamp": "2026-02-17T10:30:00"
}
```

### Error 422 (negocio)
```json
{
    "success": false,
    "message": "No se puede cerrar un ticket que ya está cerrado",
    "timestamp": "2026-02-17T10:30:00"
}
```
