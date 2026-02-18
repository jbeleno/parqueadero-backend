# Skill 12: Configuración CORS centralizada y Jackson

## CorsConfig.java

Ya documentada en `09-security-jwt-pattern.md`. Recordatorio: quitar `@CrossOrigin(origins = "*")` de TODOS los controllers.

---

## JacksonConfig.java (paquete: common.config)

Configura la serialización JSON para manejar tipos especiales.

```java
package com.usco.parqueaderos_api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

Esto asegura que `LocalDateTime`, `LocalDate`, `LocalTime` se serialicen como strings ISO-8601 (`"2026-02-17T10:30:00"`) en lugar de arrays de números.

---

## Resumen de archivos en common/config/

| Archivo | Propósito |
|---------|-----------|
| `CorsConfig.java` | CORS centralizado (reemplaza @CrossOrigin de cada controller) |
| `WebSocketConfig.java` | STOMP + SockJS |
| `JacksonConfig.java` | Serialización JSON de fechas |
