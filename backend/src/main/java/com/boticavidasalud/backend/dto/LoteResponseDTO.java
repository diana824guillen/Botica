package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteResponseDTO {
    private Integer id;
    private Integer idProducto;
    private String nombreProducto;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private BigDecimal cantidad;
    private Short estado;
    private LocalDateTime fechaIngreso;
}


