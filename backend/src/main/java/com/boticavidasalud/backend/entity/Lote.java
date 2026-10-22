package com.boticavidasalud.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lote")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "numero_lote", length = 50, nullable = false)
    private String numeroLote;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Column(name = "fecha_ingreso", updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(nullable = false)
    private Short estado = 1;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_desactivacion")
    private LocalDateTime fechaDesactivacion;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @PrePersist
    protected void onCreate() {
        fechaIngreso = LocalDateTime.now();
        if (estado == null) estado = 1;
        if (cantidad == null) cantidad = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}


