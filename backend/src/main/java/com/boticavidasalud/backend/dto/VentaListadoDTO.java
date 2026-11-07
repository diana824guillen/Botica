package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaListadoDTO {

    private Integer id;
    private String numeroNotaVenta;
    private LocalDateTime fechaVenta;
    private String clienteNombre;
    private String clienteTipoDocumento;
    private String clienteNumeroDocumento;
    private String cajeroNombre;
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal total;
    private Short estado;
}


