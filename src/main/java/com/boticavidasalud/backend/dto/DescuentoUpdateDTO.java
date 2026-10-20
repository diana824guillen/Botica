package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoCondicionDescuento;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DescuentoUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de condición es obligatorio")
    private TipoCondicionDescuento tipoCondicion;

    @NotNull(message = "El valor es obligatorio")
    @Positive(message = "El valor debe ser mayor a 0")
    private BigDecimal valorCondicion;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "100.00")
    private BigDecimal porcentaje;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}


