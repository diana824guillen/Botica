package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductoUpdateDTO {

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String descripcion;

    private Boolean requiereReceta = false;

    // Opcional: actualizar presentaciones existentes mediante sus IDs.
    @Valid
    private List<PresentacionDTO> presentaciones;
}


