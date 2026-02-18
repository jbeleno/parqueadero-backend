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

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void enviarConfirmacionCuenta(String correo, String nombre, String pin) {
        String html = buildEmail(
            "Confirma tu cuenta",
            nombre,
            "Tu código de verificación",
            "Ingresa este código en la app para activar tu cuenta. Expira en <strong>15 minutos</strong>.",
            buildPinBlock(pin),
            "#2563eb",
            buildFooterNote("Si no creaste una cuenta en Parqueaderos, puedes ignorar este correo con seguridad.")
        );
        enviar(correo, "\uD83D\uDD11 Confirma tu cuenta — Parqueaderos", html);
    }

    @Async
    public void enviarRecuperacionPassword(String correo, String nombre, String pin) {
        String html = buildEmail(
            "Recupera tu contraseña",
            nombre,
            "Código de recuperación",
            "Usa este código para restablecer tu contraseña. Expira en <strong>15 minutos</strong>.",
            buildPinBlock(pin),
            "#7c3aed",
            buildFooterNote("Si no solicitaste recuperar tu contraseña, ignora este correo. Tu cuenta sigue segura.")
        );
        enviar(correo, "\uD83D\uDD10 Código de recuperación — Parqueaderos", html);
    }

    @Async
    public void enviarCambioPasswordExitoso(String correo, String nombre) {
        String successBlock = """
            <div style="background:#ecfdf5;border:1px solid #6ee7b7;border-radius:12px;
                        padding:20px 24px;margin:24px 0;display:flex;align-items:center;gap:12px">
              <span style="font-size:2rem">\u2705</span>
              <div>
                <p style="margin:0;font-weight:600;color:#065f46;font-size:1rem">Contraseña actualizada</p>
                <p style="margin:4px 0 0;color:#047857;font-size:.875rem">
                  Tu contraseña fue cambiada exitosamente el %s.
                </p>
              </div>
            </div>
            """.formatted(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")));

        String html = buildEmail(
            "Contraseña actualizada",
            nombre,
            "Tu contraseña fue cambiada",
            "Te informamos que la contraseña de tu cuenta fue modificada correctamente.",
            successBlock,
            "#059669",
            buildFooterNote("Si NO realizaste este cambio, contacta a soporte inmediatamente respondiendo este correo.")
        );
        enviar(correo, "\u26A0\uFE0F Contraseña actualizada — Parqueaderos", html);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS — TEMPLATE ENGINE
    // ─────────────────────────────────────────────────────────────────────────

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

              <!-- Outer wrapper -->
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px">
                <tr>
                  <td align="center">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="max-width:520px;background:#ffffff;border-radius:16px;
                                  box-shadow:0 4px 24px rgba(0,0,0,.08);overflow:hidden">

                      <!-- Header band -->
                      <tr>
                        <td style="background:%s;padding:32px 40px;text-align:center">
                          <p style="margin:0;font-size:1.6rem;font-weight:700;color:#fff;letter-spacing:-.5px">
                            \uD83C\uDD7F\uFE0F Parqueaderos
                          </p>
                          <p style="margin:6px 0 0;font-size:.875rem;color:rgba(255,255,255,.75);font-weight:400">
                            Sistema de gestión de parqueaderos
                          </p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:36px 40px 28px">

                          <!-- Greeting -->
                          <p style="margin:0 0 6px;font-size:.875rem;color:#64748b;font-weight:500">
                            Hola,
                          </p>
                          <h1 style="margin:0 0 20px;font-size:1.375rem;font-weight:700;color:#0f172a">
                            %s &nbsp;<span style="color:%s">👋</span>
                          </h1>

                          <!-- Heading -->
                          <h2 style="margin:0 0 8px;font-size:1.125rem;font-weight:600;color:#1e293b">
                            %s
                          </h2>

                          <!-- Intro text -->
                          <p style="margin:0 0 20px;font-size:.9375rem;line-height:1.6;color:#475569">
                            %s
                          </p>

                          <!-- Dynamic content block (PIN or success) -->
                          %s

                          <!-- Divider -->
                          <hr style="border:none;border-top:1px solid #e2e8f0;margin:28px 0" />

                          <!-- Footer note -->
                          %s

                        </td>
                      </tr>

                      <!-- Footer bar -->
                      <tr>
                        <td style="background:#f8fafc;border-top:1px solid #e2e8f0;
                                   padding:20px 40px;text-align:center">
                          <p style="margin:0;font-size:.75rem;color:#94a3b8">
                            © 2026 Parqueaderos · USCO &nbsp;|&nbsp;
                            <a href="#" style="color:#94a3b8;text-decoration:underline">Soporte</a>
                          </p>
                          <p style="margin:6px 0 0;font-size:.75rem;color:#cbd5e1">
                            Este correo fue enviado automáticamente, por favor no respondas directamente.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>

            </body>
            </html>
            """.formatted(title, accentColor, nombre, accentColor, heading, intro, contentBlock, footer);
    }

    private String buildPinBlock(String pin) {
        // Split digits for individual styled boxes
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
                        letter-spacing:.1em;color:#64748b">Tu código de 6 dígitos</p>
              <div style="display:inline-block;background:#f1f5f9;border-radius:14px;
                          padding:18px 24px;border:1px solid #e2e8f0">
                %s
              </div>
              <p style="margin:14px 0 0;font-size:.8125rem;color:#94a3b8">
                \u23F0 Expira en 15 minutos · Uso único
              </p>
            </div>
            """.formatted(digits);
    }

    private String buildFooterNote(String note) {
        return """
            <div style="background:#fffbeb;border-left:4px solid #f59e0b;
                        border-radius:0 8px 8px 0;padding:12px 16px">
              <p style="margin:0;font-size:.8125rem;color:#92400e;line-height:1.5">
                <strong>\u26A0\uFE0F Nota de seguridad:</strong> %s
              </p>
            </div>
            """.formatted(note);
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
