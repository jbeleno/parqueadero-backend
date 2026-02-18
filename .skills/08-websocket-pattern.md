# Skill 08: WebSocket con STOMP

## Dependencia requerida en pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## 1. WebSocketConfig.java (paquete: common.config)

```java
package com.usco.parqueaderos_api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo para canales de suscripción (server → client)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para mensajes del cliente al server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para canales de usuario individual
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexión WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // Fallback para navegadores que no soportan WS nativo
        
        // Endpoint sin SockJS (para apps móviles nativas)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
```

---

## 2. Canales STOMP definidos

| Canal | Dirección | Descripción | Ejemplo de payload |
|-------|-----------|-------------|-------------------|
| `/topic/parking/{parqueaderoId}/spots` | Server → Client | Estado de puestos (libre/ocupado) | `{"puntoParqueoId": 5, "estado": "OCUPADO", "vehiculoPlaca": "ABC123"}` |
| `/topic/parking/{parqueaderoId}/tickets` | Server → Client | Eventos de tickets en tiempo real | `{"ticketId": 10, "tipo": "ENTRADA", "vehiculoPlaca": "ABC123"}` |
| `/topic/parking/{parqueaderoId}/devices` | Server → Client | Alertas de sensores/dispositivos | `{"dispositivoId": 3, "alerta": "SENSOR_OFFLINE"}` |
| `/user/{userId}/notifications` | Server → Client (privado) | Notificaciones personales | `{"tipo": "RESERVA_CONFIRMADA", "mensaje": "Tu reserva fue confirmada"}` |

---

## 3. WebSocketNotificationService.java (paquete: notification.service)

Servicio centralizado para enviar mensajes por WebSocket.

```java
package com.usco.parqueaderos_api.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Enviar actualización de estado de un puesto de parqueo.
     * Los clientes suscritos a /topic/parking/{parqueaderoId}/spots reciben esto.
     */
    public void sendSpotUpdate(Long parqueaderoId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/parking/" + parqueaderoId + "/spots",
                payload
        );
    }
    
    /**
     * Enviar evento de ticket (entrada/salida).
     * Los clientes suscritos a /topic/parking/{parqueaderoId}/tickets reciben esto.
     */
    public void sendTicketEvent(Long parqueaderoId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/parking/" + parqueaderoId + "/tickets",
                payload
        );
    }
    
    /**
     * Enviar alerta de dispositivo.
     */
    public void sendDeviceAlert(Long parqueaderoId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/parking/" + parqueaderoId + "/devices",
                payload
        );
    }
    
    /**
     * Enviar notificación privada a un usuario específico.
     * El cliente debe suscribirse a /user/{userId}/notifications.
     */
    public void sendToUser(Long userId, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/notifications",
                payload
        );
    }
    
    /**
     * Broadcast genérico a un topic personalizado.
     */
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }
}
```

---

## 4. Uso desde un Service: enviar WebSocket al crear ticket

```java
// En TicketService.java:

@Service
@RequiredArgsConstructor
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final WebSocketNotificationService wsService;
    // ... otros repos
    
    @Transactional
    public TicketDTO createTicket(TicketDTO dto) {
        Ticket ticket = toEntity(dto);
        ticket.setFechaHoraEntrada(LocalDateTime.now());
        ticket.setEstado("EN_CURSO");
        ticket = ticketRepository.save(ticket);
        
        // Enviar actualización de puesto por WebSocket
        wsService.sendSpotUpdate(ticket.getParqueadero().getId(), Map.of(
                "puntoParqueoId", ticket.getPuntoParqueo().getId(),
                "estado", "OCUPADO",
                "vehiculoPlaca", ticket.getVehiculo().getPlaca(),
                "ticketId", ticket.getId()
        ));
        
        // Enviar evento de ticket
        wsService.sendTicketEvent(ticket.getParqueadero().getId(), Map.of(
                "ticketId", ticket.getId(),
                "tipo", "ENTRADA",
                "vehiculoPlaca", ticket.getVehiculo().getPlaca(),
                "puntoParqueo", ticket.getPuntoParqueo().getNombre(),
                "timestamp", LocalDateTime.now().toString()
        ));
        
        return toDTO(ticket);
    }
    
    @Transactional
    public TicketDTO closeTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        
        if ("CERRADO".equals(ticket.getEstado())) {
            throw new BusinessException("El ticket ya está cerrado");
        }
        
        ticket.setFechaHoraSalida(LocalDateTime.now());
        ticket.setEstado("CERRADO");
        // ... calcular monto
        ticket = ticketRepository.save(ticket);
        
        // Enviar actualización: puesto libre
        wsService.sendSpotUpdate(ticket.getParqueadero().getId(), Map.of(
                "puntoParqueoId", ticket.getPuntoParqueo().getId(),
                "estado", "LIBRE",
                "ticketId", ticket.getId()
        ));
        
        return toDTO(ticket);
    }
}
```

---

## 5. Conexión desde cliente JavaScript (referencia)

```javascript
// Web (con SockJS + STOMP.js)
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Suscribirse a puestos del parqueadero 1
    stompClient.subscribe('/topic/parking/1/spots', function(message) {
        const data = JSON.parse(message.body);
        console.log('Puesto actualizado:', data);
        // { puntoParqueoId: 5, estado: "OCUPADO", vehiculoPlaca: "ABC123" }
    });
    
    // Suscribirse a notificaciones personales
    stompClient.subscribe('/user/1/notifications', function(message) {
        const data = JSON.parse(message.body);
        console.log('Notificación:', data);
    });
});
```

---

## 6. Conexión desde cliente móvil (referencia)

```
// Android/iOS: conectar via WebSocket nativo (sin SockJS)
// URL: ws://localhost:8080/ws
// Protocolo: STOMP
// Headers: Authorization: Bearer <jwt_token>
```

La autenticación del WebSocket se maneja en el JwtAuthenticationFilter (ver skill 09).
