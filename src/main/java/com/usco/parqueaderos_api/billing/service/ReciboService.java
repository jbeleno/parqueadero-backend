package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.billing.repository.PagoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera recibo en texto plano (80 columnas) para impresora termica o
 * visualizacion directa. NO es factura electronica DIAN (eso es alcance separado).
 */
@Service
@RequiredArgsConstructor
public class ReciboService {

    private static final int COLS = 48;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public String generarTxt(Long facturaId) {
        Factura f = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", facturaId));
        if (!currentUser.isSuperAdmin() && f.getParqueadero() != null
                && f.getParqueadero().getEmpresa() != null) {
            // Soft check: USER puede ver sus propios recibos; ADMIN su empresa
            if (currentUser.isAdmin()) {
                currentUser.requireEmpresa(f.getParqueadero().getEmpresa().getId());
            } else {
                Long personaId = f.getVehiculo() != null && f.getVehiculo().getPersona() != null
                        ? f.getVehiculo().getPersona().getId() : null;
                currentUser.requireOwnerOrAnyAdmin(personaId);
            }
        }

        StringBuilder sb = new StringBuilder();
        line(sb, "");
        com.usco.parqueaderos_api.parking.entity.Parqueadero parq = f.getParqueadero();
        center(sb, parq != null && parq.getEmpresa() != null
                ? parq.getEmpresa().getNombre() : "PARQUEADERO");
        if (parq != null && parq.getEmpresa() != null && parq.getEmpresa().getNit() != null) {
            center(sb, "NIT " + parq.getEmpresa().getNit());
        }
        center(sb, parq != null ? parq.getNombre() : "");
        if (parq != null && parq.getDireccion() != null) {
            center(sb, parq.getDireccion());
        }
        if (parq != null && parq.getTelefono() != null) {
            center(sb, "Tel: " + parq.getTelefono());
        }
        // Regimen tributario (editable)
        if (parq != null && parq.getRegimenTributario() != null && !parq.getRegimenTributario().isBlank()) {
            center(sb, parq.getRegimenTributario());
        }
        // Encabezado personalizado (editable, multilinea)
        if (parq != null && parq.getEncabezadoRecibo() != null && !parq.getEncabezadoRecibo().isBlank()) {
            separator(sb);
            for (String l : parq.getEncabezadoRecibo().split("\n")) center(sb, l);
        }
        separator(sb);
        center(sb, "RECIBO DE PARQUEO");
        line(sb, "Factura #" + f.getId());
        line(sb, "Fecha:  " + (f.getFechaHora() != null ? f.getFechaHora().format(FMT) : ""));
        separator(sb);

        Ticket t = f.getTicket();
        if (t != null) {
            line(sb, "Ticket #" + t.getId());
            line(sb, "Vehiculo: " + (t.getVehiculo() != null ? t.getVehiculo().getPlaca() : "?"));
            if (t.getFechaHoraEntrada() != null) {
                line(sb, "Entrada: " + t.getFechaHoraEntrada().format(FMT));
            }
            if (t.getFechaHoraSalida() != null) {
                line(sb, "Salida:  " + t.getFechaHoraSalida().format(FMT));
                long mins = Duration.between(t.getFechaHoraEntrada(), t.getFechaHoraSalida()).toMinutes();
                long h = mins / 60;
                long m = mins % 60;
                line(sb, "Duracion: " + h + "h " + m + "min");
            }
        }
        separator(sb);

        // Detalle monetario
        if (f.getBaseImponible() != null && f.getIvaMonto() != null) {
            keyValue(sb, "Base imponible", money(f.getBaseImponible()));
            String pct = f.getIvaPorcentaje() != null
                    ? String.format("%.0f", f.getIvaPorcentaje()) : "";
            keyValue(sb, "IVA " + pct + "%", money(f.getIvaMonto()));
        }
        keyValue(sb, "TOTAL", money(f.getValorTotal()));
        keyValue(sb, "Estado", f.getEstado());

        // Pagos COMPLETADO asociados
        List<Pago> pagos = pagoRepository.findByFacturaId(f.getId()).stream()
                .filter(p -> "COMPLETADO".equals(p.getEstado()))
                .toList();
        if (!pagos.isEmpty()) {
            separator(sb);
            center(sb, "PAGOS");
            for (Pago p : pagos) {
                keyValue(sb, p.getMetodo() + " " + p.getFechaHora().format(FMT),
                        money(p.getMonto()));
            }
        }

        separator(sb);
        // Pie editable + Resolucion DIAN (auditados via ConfiguracionReciboService)
        if (parq != null && parq.getResolucionDian() != null && !parq.getResolucionDian().isBlank()) {
            for (String l : parq.getResolucionDian().split("\n")) center(sb, l);
            line(sb, "");
        }
        if (parq != null && parq.getPieRecibo() != null && !parq.getPieRecibo().isBlank()) {
            for (String l : parq.getPieRecibo().split("\n")) center(sb, l);
            line(sb, "");
        }
        center(sb, "Gracias por su preferencia");
        line(sb, "");
        return sb.toString();
    }

    private void line(StringBuilder sb, String s) {
        sb.append(s).append('\n');
    }
    private void center(StringBuilder sb, String s) {
        if (s == null) s = "";
        int pad = Math.max(0, (COLS - s.length()) / 2);
        sb.append(" ".repeat(pad)).append(s).append('\n');
    }
    private void separator(StringBuilder sb) {
        sb.append("-".repeat(COLS)).append('\n');
    }
    private void keyValue(StringBuilder sb, String k, String v) {
        int spaces = Math.max(1, COLS - k.length() - v.length());
        sb.append(k).append(" ".repeat(spaces)).append(v).append('\n');
    }
    private String money(Double d) {
        if (d == null) return "$0";
        return String.format(java.util.Locale.US, "$%,.0f", d);
    }
}
