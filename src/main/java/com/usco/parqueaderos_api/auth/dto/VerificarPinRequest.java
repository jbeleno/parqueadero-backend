package com.usco.parqueaderos_api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerificarPinRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    private String correo;

    @NotBlank(message = "El PIN es obligatorio")
    @Size(min = 6, max = 6, message = "El PIN debe tener exactamente 6 dígitos")
    private String pin;
}
