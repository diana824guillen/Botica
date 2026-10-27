package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequestDTO {

    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    private String nombres;     
    private String apellidos;   
    private String correo;  
    private String origenDatos;   
}


