
package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.MetodoPago;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaResponseDTO {

    private Integer id;
    private String numeroNotaVenta;
    private LocalDateTime fechaVenta;

    private String clienteNombre;
    private String cajeroNombre;

    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal total;

    private List<DetalleVentaInfoDTO> detalles;
    private List<PagoInfoDTO> pagos;

    @Data
    public static class DetalleVentaInfoDTO {

        private String productoNombre;
        private String presentacionTipo;
        // Cantidad comercial vendida.
        private BigDecimal cantidad;
        private Integer factorConversion;
        // Equivalente descontado del inventario.
        private BigDecimal cantidadBase;
        // Precio de una presentación completa.
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        // Lote del que se retiraron las existencias.
        private String numeroLote;
    }

    @Data
    public static class PagoInfoDTO {

        private MetodoPago metodoPago;
        private BigDecimal monto;
    }
}


