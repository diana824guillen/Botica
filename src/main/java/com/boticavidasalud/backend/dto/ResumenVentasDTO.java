package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVentasDTO {
    private Long cantidadVentas;
    private BigDecimal totalRecaudado;
    private BigDecimal totalDescuentos;
}


