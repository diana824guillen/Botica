package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteUpdateDTO {

    // El servidor verificará que estos campos no se modifiquen.
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    private String apellidos;
    private String correo;
}


