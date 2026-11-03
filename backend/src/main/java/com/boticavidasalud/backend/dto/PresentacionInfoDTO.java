
package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoPresentacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresentacionInfoDTO {

    private Integer id;
    private TipoPresentacion tipo;

    private Integer factorConversion;
    private BigDecimal precioVenta;

    private BigDecimal stockMinimo;
    private BigDecimal stockDisponible;

    private Short estado;
}


