package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetalleVentaRequestDTO {
    @NotNull(message = "El ID de la presentación es obligatorio")
    private Integer idPresentacion;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;
}


