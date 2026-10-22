package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VentaRequestDTO {
    private Integer idCliente; // Opcional

    private String observacion;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;

    @NotEmpty(message = "Debe registrar al menos un método de pago")
    @Valid
    private List<PagoRequestDTO> pagos;
}


