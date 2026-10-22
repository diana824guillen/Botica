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
    private Cliente cliente; // Puede ser null si el cliente no se registra

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "descuento_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false)
    private Short estado = 1; // 1=Emitida, 0=Anulada, 2=Eliminada

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaVenta = LocalDateTime.now();
        if (estado == null) estado = 1;
        if (descuentoTotal == null) descuentoTotal = BigDecimal.ZERO;
    }
}


