package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.MotivoAjuste;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoteAjusteDTO {

    @NotNull(message = "El ID de la presentación es obligatorio")
    private Integer idPresentacion;

    @NotNull(message = "La cantidad del ajuste es obligatoria (puede ser negativa)")
    private BigDecimal cantidadAjuste;

    @NotNull(message = "Debe seleccionar un motivo de la lista")
    private MotivoAjuste motivo;

    @Size(max = 200, message = "La observación no puede superar los 200 caracteres")
    private String observacion;
}


