package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVentaPeriodoDTO {

    // Fecha YYYY-MM-DD o mes YYYY-MM.
    private String periodo;

    private BigDecimal totalVendido;

    private Long cantidadNotas;
}


