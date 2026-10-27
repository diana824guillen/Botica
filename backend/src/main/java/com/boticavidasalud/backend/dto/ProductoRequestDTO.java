package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductoRequestDTO {

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String descripcion;

    private Boolean requiereReceta = false;

    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    @Digits(
        integer = 8,
        fraction = 2,
        message = "El stock mínimo admite hasta dos decimales"
    )
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @NotEmpty(message = "Agregue al menos una presentación al producto")
    @Valid
    private List<PresentacionDTO> presentaciones = new ArrayList<>();
}


