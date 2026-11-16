package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLoteAlertaDTO {

    private Integer idLote;

    private Integer idProducto;
    private String nombreProducto;

    private String numeroLote;

    private LocalDate fechaVencimiento;

    // Días restantes para el vencimiento.
    // Será negativo si el lote ya está vencido.
    private Long diasRestantes;

    // Existencias reales por presentación del lote.
    private List<LotePresentacionInfoDTO> existenciasPresentaciones;
}


