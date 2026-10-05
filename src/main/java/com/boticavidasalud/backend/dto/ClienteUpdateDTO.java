package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteUpdateDTO {

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    private String apellidos;

    private String correo;
}


