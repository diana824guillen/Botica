package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoPresentacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPresentacionAlertaDTO {

    private Integer idProducto;
    private String nombreProducto;

    private Integer idPresentacion;
    private TipoPresentacion tipoPresentacion;

    private Integer factorConversion;

    private BigDecimal stockDisponible;
    private BigDecimal stockMinimo;
}


