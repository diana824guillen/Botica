package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoCondicionDescuento;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DescuentoRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "El nombre de la promoción es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de condición es obligatorio")
    private TipoCondicionDescuento tipoCondicion;

    @NotNull(message = "El valor de la condición es obligatorio")
    @Positive(message = "El valor debe ser mayor a 0")
    private BigDecimal valorCondicion;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor a 0")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar el 100%")
    private BigDecimal porcentaje;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}


