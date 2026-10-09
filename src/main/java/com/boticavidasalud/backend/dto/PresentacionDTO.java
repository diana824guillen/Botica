package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoPresentacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PresentacionDTO {

    private Integer id;

    @NotNull(message = "El tipo de presentación es obligatorio")
    private TipoPresentacion tipo;

    @Positive(message = "El factor de conversión debe ser mayor a 0")
    private Integer factorConversion = 1;

    @NotNull(message = "El precio de venta es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precioVenta;
}


