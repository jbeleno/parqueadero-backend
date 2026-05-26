package com.usco.parqueaderos_api.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuracion editable del recibo / ticket de un parqueadero.
 * Todos los campos son opcionales (NULL = no mostrar).
 * Cualquier cambio queda en audit_log con motivo obligatorio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionReciboDTO {
    /** Resolucion DIAN, multilinea. Ej: "Resolucion No. 1876... Vigencia X meses." */
    private String resolucionDian;
    /** Texto libre al pie (mensaje legal, terminos, etc). */
    private String pieRecibo;
    /** Texto libre al encabezado. */
    private String encabezadoRecibo;
    /** Una linea de regimen tributario. */
    private String regimenTributario;
    /** URL absoluta del logo (img). */
    private String logoUrl;
}
