
package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteResponseDTO {

    private Integer id;
    private Integer idProducto;
    private String nombreProducto;
    private String numeroLote;
    private LocalDate fechaVencimiento;

    // Stock total expresado en unidad base.
    private BigDecimal cantidad;

    // Saldos reales por presentación dentro de este lote.
    private List<LotePresentacionInfoDTO> existenciasPresentaciones;

    private Short estado;
    private LocalDateTime fechaIngreso;
}


