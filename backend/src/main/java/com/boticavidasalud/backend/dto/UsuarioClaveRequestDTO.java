
package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UsuarioClaveRequestDTO {

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(
        min = 8,
        max = 72,
        message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    private String nuevaClave;
}


