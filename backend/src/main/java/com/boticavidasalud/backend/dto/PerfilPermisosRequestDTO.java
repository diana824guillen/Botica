package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Set;

@Data
public class PerfilPermisosRequestDTO {

    @NotNull(message = "Debe enviar la lista de permisos del perfil")
    private Set<
        @NotNull(message = "El identificador del permiso no puede ser nulo")
        @Positive(message = "El identificador del permiso debe ser válido")
        Integer
    > opcionIds;
}


