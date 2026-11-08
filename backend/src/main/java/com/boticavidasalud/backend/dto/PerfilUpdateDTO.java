package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilUpdateDTO {

    @NotBlank(message = "El nombre del perfil es obligatorio")
    @Size(
        max = 50,
        message = "El nombre del perfil no puede superar los 50 caracteres"
    )
    private String nombre;

    @Size(
        max = 255,
        message = "La descripción no puede superar los 255 caracteres"
    )
    private String descripcion;
}


