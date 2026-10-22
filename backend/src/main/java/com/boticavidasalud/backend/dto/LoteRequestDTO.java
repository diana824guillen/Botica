package com.boticavidasalud.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LoteRequestDTO {

    @NotBlank(message = "El número de lote es obligatorio")
    private String numeroLote;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Future(message = "La fecha de vencimiento debe ser futura")
    private LocalDate fechaVencimiento;

    @NotEmpty(message = "Debe registrar al menos una presentación en el ingreso")
    private List<DetalleIngresoDTO> detallesIngreso;
}


