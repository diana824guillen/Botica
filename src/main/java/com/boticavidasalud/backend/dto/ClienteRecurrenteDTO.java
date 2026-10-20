package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRecurrenteDTO {
    private String nombreCompleto;
    private Long cantidadCompras;
    private BigDecimal totalGastado;
}


