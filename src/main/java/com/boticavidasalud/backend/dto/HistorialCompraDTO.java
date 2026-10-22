package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCompraDTO {
    private Integer idVenta;
    private String numeroNotaVenta;
    private LocalDateTime fechaVenta;
    private BigDecimal total;
    private Short estado;
}


