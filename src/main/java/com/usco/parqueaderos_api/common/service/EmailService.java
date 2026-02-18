package com.usco.parqueaderos_api.common.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.from-email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Async
    public void enviarConfirmacionCuenta(String correo, String nombre, String pin) {
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#1a56db">Confirma tu cuenta en Parqueaderos</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu c\u00f3digo de confirmaci\u00f3n es:</p>
                  <div style="font-size:2rem;font-weight:bold;letter-spacing:.5rem;color:#111;padding:1rem;background:#f3f4f6;border-radius:8px;text-align:center">%s</div>
                  <p style="color:#6b7280;font-size:.875rem">Este c\u00f3digo expira en 15 minutos.</p>
                </div>
                """.formatted(nombre, pin);
        enviar(correo, "Confirma tu cuenta en Parqueaderos", html);
    }

    @Async
    public void enviarRecuperacionPassword(String correo, String nombre, String pin) {
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#1a56db">Recupera tu contrase\u00f1a</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu PIN de recuperaci\u00f3n es:</p>
                  <div style="font-size:2rem;font-weight:bold;letter-spacing:.5rem;color:#111;padding:1rem;background:#f3f4f6;border-radius:8px;text-align:center">%s</div>
                  <p style="color:#6b7280;font-size:.875rem">Este c\u00f3digo expira en 15 minutos. Si no solicitaste este cambio, ignora este correo.</p>
                </div>
                """.formatted(nombre, pin);
        enviar(correo, "Recupera tu contraseña en Parqueaderos", html);
    }

    @Async
    public void enviarCambioPasswordExitoso(String correo, String nombre) {
        String html = """
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#059669">Contrase\u00f1a actualizada</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu contrase\u00f1a fue cambiada exitosamente.</p>
                  <p style="color:#6b7280;font-size:.875rem">Si no fuiste t\u00fa, contacta a soporte inmediatamente.</p>
                </div>
                """.formatted(nombre);
        enviar(correo, "Tu contraseña fue cambiada — Parqueaderos", html);
    }

    private void enviar(String para, String asunto, String htmlBody) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(para)
                    .subject(asunto)
                    .html(htmlBody)
                    .build();
            resend.emails().send(params);
            log.info("Email enviado a {} — {}", para, asunto);
        } catch (ResendException e) {
            log.error("Error enviando email a {}: {}", para, e.getMessage());
        }
    }
}
