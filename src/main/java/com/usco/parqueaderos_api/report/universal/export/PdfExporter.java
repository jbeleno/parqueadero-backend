package com.usco.parqueaderos_api.report.universal.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.usco.parqueaderos_api.report.universal.ReporteSpec;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Genera PDF tabular con OpenPDF. Diseño minimalista:
 *  - Header: titulo + filtros aplicados + generado por + fecha
 *  - Tabla con todas las columnas del spec
 *  - Footer con total de filas
 */
@Component
public class PdfExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] exportar(ReporteSpec spec, List<Map<String, Object>> filas,
                            ReporteSpec.Filtros filtros, String usuarioGen) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font bigBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

            // Titulo
            Paragraph titulo = new Paragraph(spec.titulo(), bigBold);
            titulo.setAlignment(Element.ALIGN_LEFT);
            doc.add(titulo);

            // Metadata del reporte
            StringBuilder meta = new StringBuilder();
            meta.append("Generado: ").append(LocalDateTime.now().format(FMT));
            if (usuarioGen != null) meta.append(" | Por: ").append(usuarioGen);
            if (filtros != null) {
                if (filtros.empresaId() != null) meta.append(" | Empresa: ").append(filtros.empresaId());
                if (filtros.parqueaderoId() != null) meta.append(" | Parqueadero: ").append(filtros.parqueaderoId());
                if (filtros.desde() != null) meta.append(" | Desde: ").append(filtros.desde());
                if (filtros.hasta() != null) meta.append(" | Hasta: ").append(filtros.hasta());
            }
            Paragraph metaP = new Paragraph(meta.toString(), small);
            metaP.setSpacingAfter(8f);
            doc.add(metaP);

            // Tabla
            Map<String, String> cols = spec.columnas();
            PdfPTable table = new PdfPTable(cols.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            Color headerBg = new Color(50, 60, 90);
            for (String label : cols.values()) {
                PdfPCell h = new PdfPCell(new Phrase(label,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                h.setBackgroundColor(headerBg);
                h.setPadding(5f);
                h.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(h);
            }

            int idx = 0;
            for (Map<String, Object> fila : filas) {
                Color rowBg = (idx++ % 2 == 0) ? Color.WHITE : new Color(245, 245, 250);
                for (String key : cols.keySet()) {
                    Object v = fila.get(key);
                    String txt = v == null ? "" : v.toString();
                    if (txt.length() > 60) txt = txt.substring(0, 57) + "...";
                    PdfPCell c = new PdfPCell(new Phrase(txt, normal));
                    c.setBackgroundColor(rowBg);
                    c.setPadding(4f);
                    table.addCell(c);
                }
            }
            doc.add(table);

            // Footer
            Paragraph footer = new Paragraph(
                    "Total de filas: " + filas.size(), small);
            footer.setSpacingBefore(10f);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }
}
