package com.usco.parqueaderos_api.report.universal.export;

import com.usco.parqueaderos_api.report.universal.ReporteSpec;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvExporter {

    public byte[] exportar(ReporteSpec spec, List<Map<String, Object>> filas) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Writer w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            // BOM para que Excel detecte UTF-8
            w.write('﻿');
            // Encabezado de columnas
            Map<String, String> cols = spec.columnas();
            w.write(String.join(",", cols.values().stream().map(this::escape).toList()));
            w.write("\n");
            // Filas
            for (Map<String, Object> fila : filas) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String key : cols.keySet()) {
                    if (!first) sb.append(',');
                    Object v = fila.get(key);
                    sb.append(escape(v == null ? "" : v.toString()));
                    first = false;
                }
                w.write(sb.toString());
                w.write("\n");
            }
            w.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando CSV", e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        boolean necesitaQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return necesitaQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
