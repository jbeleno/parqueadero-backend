package com.usco.parqueaderos_api.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumenSuscripcionesDTO {
    private Long parqueaderoId;
    private Long activas;
    private Long vencidas;
    private Long vencenEnSieteDias;
    private Long mensuales;
    private Long pasesDia;
    private Long abonosPrepago;
    private Double saldoTotalEnAbonos;
    private Double ingresosUltimoMes;
}
