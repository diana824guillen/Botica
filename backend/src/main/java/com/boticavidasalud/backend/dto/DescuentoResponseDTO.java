package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoCondicionDescuento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DescuentoResponseDTO {
    private Integer id;
    private String productoNombre;
    private String nombre;
    private TipoCondicionDescuento tipoCondicion;
    private BigDecimal valorCondicion;
    private BigDecimal porcentaje;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Short estado;
}


