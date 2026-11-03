package com.boticavidasalud.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
    name = "lote_presentacion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_lote_presentacion",
            columnNames = {"id_lote", "id_presentacion"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotePresentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_presentacion", nullable = false)
    private Presentacion presentacion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        if (cantidad == null) {
            cantidad = BigDecimal.ZERO;
        }
    }
}


