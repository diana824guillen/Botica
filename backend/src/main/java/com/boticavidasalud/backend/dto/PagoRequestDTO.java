package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoRequestDTO {

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    @Digits(
        integer = 8,
        fraction = 2,
        message = "El monto admite hasta dos decimales"
    )
    private BigDecimal monto;

    // Obligatorio cuando el método de pago es EFECTIVO.
    // Para los demás métodos debe enviarse como null u omitirse.
    @Digits(
        integer = 8,
        fraction = 2,
        message = "El monto recibido admite hasta dos decimales"
    )
    private BigDecimal montoRecibido;
}


