package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoDocumento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Integer id;

    private Integer perfilId;
    private String perfilNombre;

    private TipoDocumento tipoDocumento;
    private String numeroDocumento;

    private String nombres;
    private String apellidos;

    private String username;
    private String correo;

    private Short estado;

    private LocalDateTime fechaCreacion;

    private boolean bloqueado;
    private LocalDateTime bloqueoHasta;

    private boolean protegido;
}


