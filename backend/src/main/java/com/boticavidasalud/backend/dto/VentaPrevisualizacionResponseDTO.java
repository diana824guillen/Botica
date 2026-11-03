package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoCondicionDescuento;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VentaPrevisualizacionResponseDTO {

    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal total;

    // Indica si todas las líneas tienen stock suficiente.
    private boolean stockSuficiente;

    private List<DetalleInfoDTO> detalles;

    private List<DescuentoInfoDTO> descuentosAplicados;

    @Data
    public static class DetalleInfoDTO {

        private Integer idProducto;
        private Integer idPresentacion;

        private String productoNombre;
        private String presentacionTipo;

        private Integer factorConversion;

        // Cantidad comercial solicitada.
        private BigDecimal cantidad;

        // Precio de una presentación.
        private BigDecimal precioUnitario;

        private BigDecimal subtotal;

        // Saldo real disponible de esa presentación.
        private BigDecimal stockDisponible;

        private boolean stockSuficiente;
    }

    @Data
    public static class DescuentoInfoDTO {

        private Integer idDescuento;
        private String nombre;

        private String productoNombre;

        private TipoCondicionDescuento tipoCondicion;
        private BigDecimal valorCondicion;

        private BigDecimal porcentaje;

        // Importe que efectivamente se descontará.
        private BigDecimal montoDescuento;
    }
}


