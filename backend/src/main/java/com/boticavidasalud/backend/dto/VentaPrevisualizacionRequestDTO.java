package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VentaPrevisualizacionRequestDTO {

    @NotEmpty(message = "Debe agregar al menos un producto al carrito")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;
}


