package com.usco.parqueaderos_api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    @Size(max = 200)
    private String correo;

    @NotNull(message = "La persona es obligatoria")
    private Long personaId;
    private String personaNombre;
    private String personaDocumento;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;

    private Long empresaId;
    private String empresaNombre;

    private LocalDateTime fechaCreacion;
}
