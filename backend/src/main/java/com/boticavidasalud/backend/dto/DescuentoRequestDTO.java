package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoCondicionDescuento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DescuentoRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "El nombre de la promoción es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "Debe seleccionar un tipo de condición")
    private TipoCondicionDescuento tipoCondicion;

    @NotNull(message = "El valor de la condición es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El valor de la condición debe ser mayor a 0"
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message = "El valor de la condición admite hasta 8 enteros y 2 decimales"
    )
    private BigDecimal valorCondicion;

    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El porcentaje debe ser mayor a 0"
    )
    @DecimalMax(
            value = "100.00",
            message = "El porcentaje no puede superar el 100%"
    )
    @Digits(
            integer = 3,
            fraction = 2,
            message = "El porcentaje admite hasta 2 decimales"
    )
    private BigDecimal porcentaje;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;
}


