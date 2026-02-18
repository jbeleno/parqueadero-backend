# Skill 11: Servicio de Notificaciones

## Propósito
Centralizar el envío de notificaciones por múltiples canales:
1. **WebSocket** (in-app, tiempo real) — ya configurado en skill 08
2. **Firebase Cloud Messaging** (push móvil) — se puede agregar después

---

## 1. NotificationDTO.java (paquete: notification.dto)

```java
package com.usco.parqueaderos_api.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private String tipo;          // TICKET_CERRADO, RESERVA_CONFIRMADA, PAGO_EXITOSO, ALERTA, etc.
    private String titulo;
    private String mensaje;
    private Long usuarioId;
    private Long parqueaderoId;
    private LocalDateTime timestamp;
    
    // Datos adicionales según el tipo
    private Long referenciaId;    // ticketId, reservaId, facturaId, etc.
    private String referenciaTipo; // TICKET, RESERVA, FACTURA, etc.
}
```

---

## 2. NotificationService.java (paquete: notification.service)

Servicio de alto nivel que orquesta los canales.

```java
package com.usco.parqueaderos_api.notification.service;

import com.usco.parqueaderos_api.notification.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final WebSocketNotificationService webSocketService;
    // private final FirebasePushService firebasePushService; // Agregar después
    
    /**
     * Enviar notificación por todos los canales disponibles.
     */
    public void notify(NotificationDTO notification) {
        notification.setTimestamp(LocalDateTime.now());
        
        // Canal 1: WebSocket (in-app)
        sendWebSocket(notification);
        
        // Canal 2: Push (Firebase) — descomentar cuando se integre
        // sendPush(notification);
        
        log.info("Notificación enviada - tipo: {}, usuario: {}", 
                notification.getTipo(), notification.getUsuarioId());
    }
    
    /**
     * Enviar solo por WebSocket (para actualizaciones de UI en tiempo real).
     */
    public void notifyRealtime(NotificationDTO notification) {
        notification.setTimestamp(LocalDateTime.now());
        sendWebSocket(notification);
    }
    
    private void sendWebSocket(NotificationDTO notification) {
        try {
            if (notification.getUsuarioId() != null) {
                webSocketService.sendToUser(notification.getUsuarioId(), 
                    java.util.Map.of(
                        "tipo", notification.getTipo(),
                        "titulo", notification.getTitulo() != null ? notification.getTitulo() : "",
                        "mensaje", notification.getMensaje(),
                        "referenciaId", notification.getReferenciaId() != null ? notification.getReferenciaId() : 0,
                        "referenciaTipo", notification.getReferenciaTipo() != null ? notification.getReferenciaTipo() : "",
                        "timestamp", notification.getTimestamp().toString()
                    ));
            }
        } catch (Exception e) {
            log.error("Error enviando notificación WebSocket: {}", e.getMessage());
        }
    }
}
```

---

## 3. Uso desde un EventListener

```java
@EventListener
public void onTicketClosed(TicketClosedEvent event) {
    if (event.getUsuarioId() != null) {
        notificationService.notify(NotificationDTO.builder()
                .tipo("TICKET_CERRADO")
                .titulo("Ticket cerrado")
                .mensaje("Tu ticket ha sido cerrado. Monto: $" + event.getMontoCalculado())
                .usuarioId(event.getUsuarioId())
                .parqueaderoId(event.getParqueaderoId())
                .referenciaId(event.getTicketId())
                .referenciaTipo("TICKET")
                .build());
    }
}
```

---

## 4. Tipos de notificación definidos

| Tipo | Cuándo se envía | Canal |
|------|-----------------|-------|
| `TICKET_CERRADO` | Al cerrar un ticket | WebSocket + Push |
| `RESERVA_CREADA` | Al crear una reserva | WebSocket + Push |
| `RESERVA_CONFIRMADA` | Al confirmar una reserva | WebSocket + Push |
| `RESERVA_CANCELADA` | Al cancelar una reserva | WebSocket + Push |
| `RESERVA_EXPIRADA` | Cuando una reserva expira | WebSocket + Push |
| `PAGO_EXITOSO` | Al completar un pago | WebSocket + Push |
| `PAGO_FALLIDO` | Cuando falla un pago | WebSocket + Push |
| `ALERTA_DISPOSITIVO` | Alerta de sensor/dispositivo | Solo WebSocket |
| `SPOT_DISPONIBLE` | Cuando un puesto se libera (si el usuario esperaba) | WebSocket + Push |

---

## 5. Firebase Cloud Messaging (para implementar después)

Cuando se quiera agregar push notifications:

### Dependencia
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.4.3</version>
</dependency>
```

### Tabla nueva necesaria (agregar a la entidad Usuario o crear nueva)
```sql
-- Almacenar tokens de dispositivos móviles
CREATE TABLE device_token (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    token VARCHAR(500) NOT NULL,
    plataforma VARCHAR(20),  -- ANDROID, IOS, WEB
    activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT NOW()
);
```

Esto se puede implementar en una fase posterior. La arquitectura de `NotificationService` ya está preparada para agregar el canal de push sin cambiar los publicadores de eventos.
