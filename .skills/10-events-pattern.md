# Skill 10: Sistema de Eventos Internos (Spring Events)

## Propósito
Desacoplar módulos que necesitan reaccionar a acciones en otros módulos.
Ejemplo: cuando se cierra un ticket → se genera factura → se envía notificación → se actualiza mapa WebSocket.

Sin eventos, el `TicketService` tendría que inyectar `FacturaService`, `NotificationService`, y `WebSocketNotificationService` → acoplamiento fuerte.

Con eventos, el `TicketService` solo publica un evento y los listeners de cada módulo reaccionan independientemente.

---

## 1. Definir eventos (paquete: common.event)

### TicketClosedEvent.java
```java
package com.usco.parqueaderos_api.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketClosedEvent {
    private Long ticketId;
    private Long parqueaderoId;
    private Long puntoParqueoId;
    private Long vehiculoId;
    private Long usuarioId;     // puede ser null si el ticket no tiene usuario asociado
    private String vehiculoPlaca;
    private Double montoCalculado;
}
```

### ReservationCreatedEvent.java
```java
package com.usco.parqueaderos_api.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReservationCreatedEvent {
    private Long reservaId;
    private Long parqueaderoId;
    private Long puntoParqueoId;  // puede ser null
    private Long usuarioId;
    private Long vehiculoId;
    private String estado;         // PENDIENTE, CONFIRMADA
}
```

### SpotStatusChangedEvent.java
```java
package com.usco.parqueaderos_api.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpotStatusChangedEvent {
    private Long parqueaderoId;
    private Long puntoParqueoId;
    private String nuevoEstado;        // LIBRE, OCUPADO, RESERVADO, FUERA_DE_SERVICIO
    private String vehiculoPlaca;      // null si está libre
    private Long ticketId;             // null si no hay ticket
}
```

### TicketCreatedEvent.java
```java
package com.usco.parqueaderos_api.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long ticketId;
    private Long parqueaderoId;
    private Long puntoParqueoId;
    private Long vehiculoId;
    private String vehiculoPlaca;
}
```

---

## 2. Publicar eventos desde Services

```java
package com.usco.parqueaderos_api.ticket.service;

import com.usco.parqueaderos_api.common.event.TicketClosedEvent;
import com.usco.parqueaderos_api.common.event.TicketCreatedEvent;
import com.usco.parqueaderos_api.common.event.SpotStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;
    // ... otros repos
    
    @Transactional
    public TicketDTO createTicket(TicketDTO dto) {
        // ... crear ticket
        Ticket ticket = ticketRepository.save(toEntity(dto));
        
        // Publicar eventos
        eventPublisher.publishEvent(new TicketCreatedEvent(
                ticket.getId(),
                ticket.getParqueadero().getId(),
                ticket.getPuntoParqueo().getId(),
                ticket.getVehiculo().getId(),
                ticket.getVehiculo().getPlaca()
        ));
        
        eventPublisher.publishEvent(new SpotStatusChangedEvent(
                ticket.getParqueadero().getId(),
                ticket.getPuntoParqueo().getId(),
                "OCUPADO",
                ticket.getVehiculo().getPlaca(),
                ticket.getId()
        ));
        
        return toDTO(ticket);
    }
    
    @Transactional
    public TicketDTO closeTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        
        // ... cerrar ticket, calcular monto
        ticket.setEstado("CERRADO");
        ticket.setFechaHoraSalida(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);
        
        // Publicar eventos
        eventPublisher.publishEvent(new TicketClosedEvent(
                ticket.getId(),
                ticket.getParqueadero().getId(),
                ticket.getPuntoParqueo().getId(),
                ticket.getVehiculo().getId(),
                null,  // usuarioId — buscar si existe
                ticket.getVehiculo().getPlaca(),
                ticket.getMontoCalculado()
        ));
        
        eventPublisher.publishEvent(new SpotStatusChangedEvent(
                ticket.getParqueadero().getId(),
                ticket.getPuntoParqueo().getId(),
                "LIBRE",
                null,
                ticket.getId()
        ));
        
        return toDTO(ticket);
    }
}
```

---

## 3. Listeners que reaccionan a eventos

### BillingEventListener.java (paquete: billing.listener)
```java
package com.usco.parqueaderos_api.billing.listener;

import com.usco.parqueaderos_api.billing.service.FacturaService;
import com.usco.parqueaderos_api.common.event.TicketClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingEventListener {
    
    private final FacturaService facturaService;
    
    @EventListener
    public void onTicketClosed(TicketClosedEvent event) {
        log.info("Generando factura para ticket {}", event.getTicketId());
        facturaService.generateFromTicket(
                event.getTicketId(),
                event.getParqueaderoId(),
                event.getVehiculoId(),
                event.getMontoCalculado()
        );
    }
}
```

### NotificationEventListener.java (paquete: notification.listener)
```java
package com.usco.parqueaderos_api.notification.listener;

import com.usco.parqueaderos_api.common.event.*;
import com.usco.parqueaderos_api.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final WebSocketNotificationService wsService;
    
    @EventListener
    public void onSpotStatusChanged(SpotStatusChangedEvent event) {
        log.info("Spot {} cambió a {}", event.getPuntoParqueoId(), event.getNuevoEstado());
        wsService.sendSpotUpdate(event.getParqueaderoId(), Map.of(
                "puntoParqueoId", event.getPuntoParqueoId(),
                "estado", event.getNuevoEstado(),
                "vehiculoPlaca", event.getVehiculoPlaca() != null ? event.getVehiculoPlaca() : "",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    @EventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        wsService.sendTicketEvent(event.getParqueaderoId(), Map.of(
                "ticketId", event.getTicketId(),
                "tipo", "ENTRADA",
                "vehiculoPlaca", event.getVehiculoPlaca(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
    
    @EventListener
    public void onTicketClosed(TicketClosedEvent event) {
        wsService.sendTicketEvent(event.getParqueaderoId(), Map.of(
                "ticketId", event.getTicketId(),
                "tipo", "SALIDA",
                "vehiculoPlaca", event.getVehiculoPlaca(),
                "monto", event.getMontoCalculado(),
                "timestamp", LocalDateTime.now().toString()
        ));
        
        // Notificación personal al usuario (si tiene)
        if (event.getUsuarioId() != null) {
            wsService.sendToUser(event.getUsuarioId(), Map.of(
                    "tipo", "TICKET_CERRADO",
                    "mensaje", "Tu ticket ha sido cerrado. Monto: $" + event.getMontoCalculado(),
                    "ticketId", event.getTicketId()
            ));
        }
    }
    
    @EventListener
    public void onReservationCreated(ReservationCreatedEvent event) {
        wsService.sendToUser(event.getUsuarioId(), Map.of(
                "tipo", "RESERVA_CREADA",
                "mensaje", "Tu reserva ha sido creada exitosamente",
                "reservaId", event.getReservaId(),
                "estado", event.getEstado()
        ));
    }
}
```

---

## Diagrama de flujo de eventos

```
TICKET CREADO:
  TicketService.createTicket()
    → publish TicketCreatedEvent
        → NotificationEventListener: envía WS /topic/parking/{id}/tickets
    → publish SpotStatusChangedEvent(OCUPADO)
        → NotificationEventListener: envía WS /topic/parking/{id}/spots

TICKET CERRADO:
  TicketService.closeTicket()
    → publish TicketClosedEvent
        → BillingEventListener: genera Factura
        → NotificationEventListener: envía WS ticket + notificación personal
    → publish SpotStatusChangedEvent(LIBRE)
        → NotificationEventListener: envía WS /topic/parking/{id}/spots

RESERVA CREADA:
  ReservaService.create()
    → publish ReservationCreatedEvent
        → NotificationEventListener: envía notificación personal al usuario
```

---

## Reglas para eventos

1. Los eventos son objetos inmutables (solo lectura, `@AllArgsConstructor` + `@Data`)
2. Se publican con `ApplicationEventPublisher.publishEvent()`
3. Los listeners usan `@EventListener` (no `@TransactionalEventListener` a menos que necesites ejecutar después del commit)
4. Los listeners NO lanzan excepciones hacia arriba — usan try/catch y log.error internamente
5. Un evento puede tener múltiples listeners en diferentes módulos
6. NUNCA crear dependencias circulares vía eventos — el publicador no conoce a los listeners
