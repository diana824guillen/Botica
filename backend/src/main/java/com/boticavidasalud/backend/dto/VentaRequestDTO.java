
package com.boticavidasalud.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class VentaRequestDTO {

    // Cliente que ya existe en la base de datos. Es opcional.
    private Integer idCliente;

    // Datos de un cliente nuevo que se registrará dentro de la misma transacción de la venta.
    // Es opcional.
    @Valid
    private ClienteRequestDTO nuevoCliente;

    @Size(
        max = 255,
        message = "La observación no puede superar los 255 caracteres"
    )
    private String observacion;

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;

    // Puede estar vacío cuando el total de la venta sea cero.
    @Valid
    private List<PagoRequestDTO> pagos = new ArrayList<>();

    // Total que el usuario visualizó antes de confirmar la venta.
    @NotNull(
        message = "Debes enviar el total calculado antes de confirmar la venta"
    )
    @DecimalMin(
        value = "0.00",
        message = "El total esperado no puede ser negativo"
    )
    @Digits(
        integer = 8,
        fraction = 2,
        message = "El total esperado admite hasta dos decimales"
    )
    private BigDecimal totalEsperado;

}


