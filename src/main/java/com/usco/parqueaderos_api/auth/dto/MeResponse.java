package com.usco.parqueaderos_api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private String correo;
    private String nombreCompleto;
    private List<String> roles;
    private Boolean confirmado;
    private LocalDateTime fechaCreacion;
}
