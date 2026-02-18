package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DispositivoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String modelo;
    private Long tipoDispositivoId;
    private String tipoDispositivoNombre;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long estadoId;
    private String estadoNombre;
    private LocalDate fechaInstalacion;
}
