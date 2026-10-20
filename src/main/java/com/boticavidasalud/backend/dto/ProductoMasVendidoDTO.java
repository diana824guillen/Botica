package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMasVendidoDTO {
    private String nombreProducto;
    private BigDecimal cantidadVendida; // En unidad base
}


