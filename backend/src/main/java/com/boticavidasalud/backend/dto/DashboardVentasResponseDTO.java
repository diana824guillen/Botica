package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVentasResponseDTO {

    // Periodo actual
    private BigDecimal totalVendido;
    private Long cantidadNotas;

    // Periodo de comparación
    private BigDecimal totalPeriodoComparacion;
    private Long cantidadNotasComparacion;

    // Variación porcentual respecto al periodo anterior.
    // Será null cuando el periodo anterior no tenga ventas.
    private BigDecimal variacionPorcentaje;

    // DIA o MES, según la duración del periodo consultado.
    private String agrupacion;

    private List<DashboardVentaPeriodoDTO> ventasPorPeriodo;

    private List<DashboardMetodoPagoDTO> metodosPago;

    private List<DashboardProductoFrecuenteDTO> productosMasSolicitados;
}


