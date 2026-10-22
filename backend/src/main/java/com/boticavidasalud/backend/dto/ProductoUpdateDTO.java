package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoUpdateDTO {

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;

    private String descripcion;

    private Boolean requiereReceta = false;

    @Positive(message = "El stock mínimo debe ser mayor a 0")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // Presentaciones (opcional, si se envían se reemplazan las anteriores)
    private List<PresentacionDTO> presentaciones;
}


