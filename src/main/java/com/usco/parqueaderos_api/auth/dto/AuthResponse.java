package com.usco.parqueaderos_api.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String correo;
    private String nombreCompleto;
    private List<String> roles;
    private String tipo;
    private Long expiresIn;
    private Long empresaId;
    private String empresaNombre;
}
