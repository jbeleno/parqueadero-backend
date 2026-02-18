package com.usco.parqueaderos_api.notification.service;

import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía notificación a todos los suscriptores del topic de un parqueadero.
     * Ruta: /topic/parqueadero/{parqueaderoId}
     */
    public void notificarParqueadero(Long parqueaderoId, NotificacionDTO notificacion) {
        String destino = "/topic/parqueadero/" + parqueaderoId;
        messagingTemplate.convertAndSend(destino, notificacion);
        log.info("Notificación enviada a {}: {} - id={}", destino, notificacion.getTipo(), notificacion.getReferenciaId());
    }

    /**
     * Envía notificación a un usuario específico.
     * Ruta: /queue/usuario/{usuarioId}
     */
    public void notificarUsuario(Long usuarioId, NotificacionDTO notificacion) {
        String destino = "/queue/usuario/" + usuarioId;
        messagingTemplate.convertAndSend(destino, notificacion);
        log.info("Notificación enviada a {}: {} - id={}", destino, notificacion.getTipo(), notificacion.getReferenciaId());
    }

    /**
     * Stub para FCM (Firebase Cloud Messaging).
     * TODO: implementar integración real con Firebase Admin SDK.
     */
    public void enviarPushFCM(String fcmToken, String titulo, String cuerpo) {
        log.info("[FCM STUB] Token={} | {} - {}", fcmToken, titulo, cuerpo);
    }
}
