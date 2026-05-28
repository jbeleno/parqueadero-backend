package com.usco.parqueaderos_api.convenio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvenioDTO {
    private Long id;
    private Long parqueaderoId;
    private String nombreComercio;
    private String nitComercio;
    private String tipoDescuento; // MONTO_FIJO | PORCENTAJE | MINUTOS_GRATIS
    private Double valorDescuento;
    private Double porcentajeDescuento;
    private Integer minutosGratis;
    private Double montoMinimoCompra;
    private LocalDateTime fechaInicioVigencia;
    private LocalDateTime fechaFinVigencia;
    private Boolean activo;
    private Long creadoPorUsuarioId;
    private String creadoPorUsuarioNombre;
    private Long desactivadoPorUsuarioId;
    private String desactivadoPorUsuarioNombre;
    private LocalDateTime desactivadoEn;
}
