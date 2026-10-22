package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String username;
    private String perfil;  
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
}


