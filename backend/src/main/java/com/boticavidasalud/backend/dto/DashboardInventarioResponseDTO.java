package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInventarioResponseDTO {

    // Horizonte seleccionado para los próximos vencimientos.
    private Integer diasAnticipacion;

    // Indicadores de presentaciones.
    private Long cantidadPresentacionesAgotadas;
    private Long cantidadPresentacionesPorReponer;

    // Indicadores de lotes.
    private Long cantidadLotesVencidos;
    private Long cantidadLotesProximosAVencer;

    // Detalle de presentaciones agotadas.
    private List<DashboardPresentacionAlertaDTO>
            presentacionesAgotadas;

    // Detalle de presentaciones por reponer.
    private List<DashboardPresentacionAlertaDTO>
            presentacionesPorReponer;

    // Detalle de lotes vencidos.
    private List<DashboardLoteAlertaDTO>
            lotesVencidos;

    // Detalle de lotes próximos a vencer.
    private List<DashboardLoteAlertaDTO>
            lotesProximosAVencer;
}


