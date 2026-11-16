package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetodoPagoDTO {

    private String metodoPago;

    // Importe realmente aplicado a las ventas.
    private BigDecimal monto;

    // Cantidad de registros de pago de este método.
    private Long cantidadPagos;

    // Porcentaje respecto al total vendido.
    private BigDecimal porcentaje;
}


