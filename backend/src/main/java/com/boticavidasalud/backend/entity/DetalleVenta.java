package com.boticavidasalud.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "id_presentacion", nullable = false)
    private Presentacion presentacion;

    @ManyToOne
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    // Cantidad descontada del inventario en unidades base.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // =====================================================
    // DATOS HISTÓRICOS DEL PRODUCTO VENDIDO
    // =====================================================

    @Column(name = "producto_nombre_snapshot", length = 150)
    private String productoNombreSnapshot;

    @Column(name = "presentacion_tipo_snapshot", length = 30)
    private String presentacionTipoSnapshot;

    @Column(name = "factor_conversion_snapshot")
    private Integer factorConversionSnapshot;

    // Cantidad comercial vendida en esta parte del lote.
    @Column(name = "cantidad_presentacion", precision = 10, scale = 2)
    private BigDecimal cantidadPresentacion;

    @Column(name = "numero_lote_snapshot", length = 50)
    private String numeroLoteSnapshot;

    @Column(nullable = false)
    private Short estado = 1;
}


