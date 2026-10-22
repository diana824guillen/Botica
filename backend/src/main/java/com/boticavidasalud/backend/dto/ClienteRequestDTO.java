package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequestDTO {

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    private String nombres;     
    private String apellidos;   
    private String correo;      
}


