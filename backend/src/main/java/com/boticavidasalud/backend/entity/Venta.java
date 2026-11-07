package com.boticavidasalud.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_nota_venta", length = 20, nullable = false, unique = true)
    private String numeroNotaVenta;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    // =====================================================
    // DATOS HISTÓRICOS DE LA VENTA
    // =====================================================

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "cliente_nombre_snapshot", length = 301)
    private String clienteNombreSnapshot;

    @Column(name = "cliente_tipo_documento_snapshot", length = 20)
    private String clienteTipoDocumentoSnapshot;

    @Column(name = "cliente_numero_documento_snapshot", length = 15)
    private String clienteNumeroDocumentoSnapshot;

    @Column(name = "cajero_nombre_snapshot", length = 201)
    private String cajeroNombreSnapshot;

    // =====================================================
    // IMPORTES DE LA VENTA
    // =====================================================

    @Column(name = "descuento_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private Short estado = 1;

    // =====================================================
    // RELACIONES
    // =====================================================

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        fechaVenta = LocalDateTime.now();

        if (estado == null) {
            estado = 1;
        }

        if (descuentoTotal == null) {
            descuentoTotal = BigDecimal.ZERO;
        }
    }
}


