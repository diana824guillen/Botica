package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LoteRequestDTO {

    @NotBlank(message = "El número de lote es obligatorio")
    @Size(
        max = 50,
        message = "El número de lote no puede superar los 50 caracteres"
    )
    private String numeroLote;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Future(message = "La fecha de vencimiento debe ser futura")
    private LocalDate fechaVencimiento;

    @NotEmpty(
        message = "Debe registrar al menos una presentación en el ingreso"
    )
    @Valid
    private List<DetalleIngresoDTO> detallesIngreso;
}


