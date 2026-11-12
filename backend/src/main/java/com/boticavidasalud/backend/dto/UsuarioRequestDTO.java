package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoDocumento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UsuarioRequestDTO {

    @NotNull(message = "Debe seleccionar un perfil")
    private Integer perfilId;

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(
        max = 15,
        message = "El documento no puede superar los 15 caracteres"
    )
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(
        max = 100,
        message = "Los nombres no pueden superar los 100 caracteres"
    )
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(
        max = 100,
        message = "Los apellidos no pueden superar los 100 caracteres"
    )
    private String apellidos;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(
        min = 4,
        max = 50,
        message = "El usuario debe tener entre 4 y 50 caracteres"
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre de usuario contiene caracteres no permitidos"
    )
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        min = 8,
        max = 72,
        message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    private String clave;

    @Size(
        max = 255,
        message = "El correo no puede superar los 255 caracteres"
    )
    @Pattern(
        regexp = "^$|^\\S+@\\S+\\.\\S+$",
        message = "El correo electrónico no tiene un formato válido"
    )
    private String correo;
}


