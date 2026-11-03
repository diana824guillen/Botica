
package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoPresentacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotePresentacionInfoDTO {

    private Integer idPresentacion;
    private TipoPresentacion tipo;
    private Integer factorConversion;
    private BigDecimal cantidad;
}


