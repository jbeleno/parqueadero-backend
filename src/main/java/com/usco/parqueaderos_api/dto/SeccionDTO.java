package com.usco.parqueaderos_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeccionDTO {
    private Long id;
    private String nombre;
    private String acronimo;
    private String descripcion;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long nivelId;
    private String nivelNombre;
    private Long estadoId;
    private String estadoNombre;
}
