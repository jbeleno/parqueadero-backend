# INICIO RÁPIDO PARA EL MODELO PROGRAMADOR

## Lee estos archivos en este orden ANTES de escribir código

1. **`ARCHITECTURE_PLAN.md`** — Plan completo de reestructuración con el orden de fases
2. **`.skills/00-project-conventions.md`** — Convenciones de nombres, paquetes, módulos
3. **`.skills/06-exception-handling.md`** — Excepciones + ApiResponse (crear PRIMERO)
4. **`.skills/01-entity-pattern.md`** — Cómo están las entidades y a qué paquete moverlas
5. **`.skills/03-repository-pattern.md`** — Repositorios existentes + nuevos query methods
6. **`.skills/02-dto-pattern.md`** — DTOs con validación
7. **`.skills/04-service-pattern.md`** — Patrón de servicio (CRÍTICO — leer con cuidado)
8. **`.skills/05-controller-pattern.md`** — Patrón de controller con ApiResponse + @Valid
9. **`.skills/07-validation-pattern.md`** — Bean Validation
10. **`.skills/08-websocket-pattern.md`** — WebSocket STOMP
11. **`.skills/09-security-jwt-pattern.md`** — JWT + Spring Security
12. **`.skills/10-events-pattern.md`** — Spring Events entre módulos
13. **`.skills/11-notification-pattern.md`** — Servicio de notificaciones
14. **`.skills/12-config-patterns.md`** — Configuraciones transversales

---

## Orden de ejecución de las fases

```
FASE 1 → Agregar dependencias al pom.xml
FASE 2 → Crear common/exception/ y common/dto/ApiResponse.java
FASE 3 → Mover archivos a estructura package-by-feature
FASE 4 → Crear capa de servicio para todos los módulos
FASE 5 → Reescribir controllers con el nuevo patrón
FASE 6 → Agregar validaciones a DTOs y entidades
FASE 7 → Actualizar application.yaml (variables de entorno)
FASE 8 → Configurar WebSocket (STOMP)
FASE 9 → Implementar seguridad (JWT + Spring Security)
FASE 10 → Implementar sistema de eventos
FASE 11 → Implementar notificaciones
```

---

## Reglas que NUNCA debes romper

1. **Controller → Service → Repository** (nunca Controller → Repository)
2. **`@Valid` en todo `@RequestBody`**
3. **`ResourceNotFoundException` cuando `findById` retorna vacío**
4. **Cargar FKs desde repositorios en `toEntity()`**
5. **`@Transactional` en servicios**
6. **`ApiResponse<T>` como wrapper de respuesta**
7. **No credenciales hardcodeadas**
8. **CORS centralizado, no en cada controller**
9. **`201 CREATED` en POST, `204 NO CONTENT` en DELETE**
10. **DTOs para entidades con FKs complejas**
