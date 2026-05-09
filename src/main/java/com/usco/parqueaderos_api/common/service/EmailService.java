package com.usco.parqueaderos_api.common.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * SINCRONO: si el envio falla, lanza BusinessException para que la
     * transaccion del registro/recuperacion haga rollback. El usuario
     * NO debe quedar registrado sin poder recibir el PIN.
     */
    public void enviarConfirmacionCuenta(String correo, String nombre, String pin) {
        String html = buildEmail(
            "Confirma tu cuenta",
            nombre,
            "Tu codigo de verificacion",
            "Ingresa este codigo en la app para activar tu cuenta. Expira en <strong>15 minutos</strong>.",
            buildPinBlock(pin),
            "#2563eb",
            buildFooterNote("Si no creaste una cuenta en Parqueaderos, puedes ignorar este correo con seguridad.")
        );
        enviar(correo, "Confirma tu cuenta - Parqueaderos", html);
    }

    /** SINCRONO: ver enviarConfirmacionCuenta. */
    public void enviarRecuperacionPassword(String correo, String nombre, String pin) {
        String html = buildEmail(
            "Recupera tu contrasena",
            nombre,
            "Codigo de recuperacion",
            "Usa este codigo para restablecer tu contrasena. Expira en <strong>15 minutos</strong>.",
            buildPinBlock(pin),
            "#7c3aed",
            buildFooterNote("Si no solicitaste recuperar tu contrasena, ignora este correo. Tu cuenta sigue segura.")
        );
        enviar(correo, "Codigo de recuperacion - Parqueaderos", html);
    }

    /** ASINC: notificacion informativa, no critica. Si falla NO se hace rollback. */
    @Async
    public void enviarCambioPasswordExitoso(String correo, String nombre) {
        String successBlock = """
            <div style="background:#ecfdf5;border:1px solid #6ee7b7;border-radius:12px;
                        padding:20px 24px;margin:24px 0;display:flex;align-items:center;gap:12px">
              <div>
                <p style="margin:0;font-weight:600;color:#065f46;font-size:1rem">Contrasena actualizada</p>
                <p style="margin:4px 0 0;color:#047857;font-size:.875rem">
                  Tu contrasena fue cambiada exitosamente el %s.
                </p>
              </div>
            </div>
            """.formatted(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")));

        String html = buildEmail(
            "Contrasena actualizada",
            nombre,
            "Tu contrasena fue cambiada",
            "Te informamos que la contrasena de tu cuenta fue modificada correctamente.",
            successBlock,
            "#059669",
            buildFooterNote("Si NO realizaste este cambio, contacta a soporte inmediatamente.")
        );
        enviar(correo, "Contrasena actualizada - Parqueaderos", html);
    }

    private String buildEmail(String title, String nombre, String heading,
                              String intro, String contentBlock,
                              String accentColor, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px">
                <tr>
                  <td align="center">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="max-width:520px;background:#ffffff;border-radius:16px;
                                  box-shadow:0 4px 24px rgba(0,0,0,.08);overflow:hidden">
                      <tr>
                        <td style="background:%s;padding:32px 40px;text-align:center">
                          <p style="margin:0;font-size:1.6rem;font-weight:700;color:#fff;letter-spacing:-.5px">
                            Parqueaderos
                          </p>
                          <p style="margin:6px 0 0;font-size:.875rem;color:rgba(255,255,255,.75);font-weight:400">
                            Sistema de gestion de parqueaderos
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px 40px 28px">
                          <p style="margin:0 0 6px;font-size:.875rem;color:#64748b;font-weight:500">Hola,</p>
                          <h1 style="margin:0 0 20px;font-size:1.375rem;font-weight:700;color:#0f172a">%s</h1>
                          <h2 style="margin:0 0 8px;font-size:1.125rem;font-weight:600;color:#1e293b">%s</h2>
                          <p style="margin:0 0 20px;font-size:.9375rem;line-height:1.6;color:#475569">%s</p>
                          %s
                          <hr style="border:none;border-top:1px solid #e2e8f0;margin:28px 0" />
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center">
                          <p style="margin:0;font-size:.75rem;color:#94a3b8">
                            2026 Parqueaderos - USCO
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(title, accentColor, nombre, heading, intro, contentBlock, footer);
    }

    private String buildPinBlock(String pin) {
        StringBuilder digits = new StringBuilder();
        for (char c : pin.toCharArray()) {
            digits.append("""
                <span style="display:inline-block;width:44px;height:52px;line-height:52px;
                             background:#f8fafc;border:2px solid #e2e8f0;border-radius:10px;
                             font-size:1.5rem;font-weight:700;color:#1e293b;text-align:center;
                             margin:0 4px;box-shadow:0 1px 3px rgba(0,0,0,.06)">%c</span>
                """.formatted(c));
        }
        return """
            <div style="text-align:center;margin:24px 0">
              <p style="margin:0 0 12px;font-size:.75rem;font-weight:600;text-transform:uppercase;
                        letter-spacing:.1em;color:#64748b">Tu codigo de 6 digitos</p>
              <div style="display:inline-block;background:#f1f5f9;border-radius:14px;
                          padding:18px 24px;border:1px solid #e2e8f0">
                %s
              </div>
              <p style="margin:14px 0 0;font-size:.8125rem;color:#94a3b8">
                Expira en 15 minutos - Uso unico
              </p>
            </div>
            """.formatted(digits);
    }

    private String buildFooterNote(String note) {
        return """
            <div style="background:#fffbeb;border-left:4px solid #f59e0b;
                        border-radius:0 8px 8px 0;padding:12px 16px">
              <p style="margin:0;font-size:.8125rem;color:#92400e;line-height:1.5">
                <strong>Nota de seguridad:</strong> %s
              </p>
            </div>
            """.formatted(note);
    }

    private void enviar(String para, String asunto, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email enviado a {} - {}", para, asunto);
        } catch (MessagingException e) {
            log.error("CRITICO: fallo envio de email a {} ({}): {}", para, asunto, e.getMessage(), e);
            throw new BusinessException(
                    "No se pudo enviar el correo. Intenta de nuevo en unos minutos.",
                    "ERR_EMAIL_DELIVERY");
        } catch (org.springframework.mail.MailException e) {
            log.error("CRITICO: fallo SMTP a {} ({}): {}", para, asunto, e.getMessage(), e);
            throw new BusinessException(
                    "No se pudo enviar el correo. Intenta de nuevo en unos minutos.",
                    "ERR_EMAIL_DELIVERY");
        }
    }
}
