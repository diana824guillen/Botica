package com.boticavidasalud.backend.dto;

import com.boticavidasalud.backend.enums.TipoMovimiento;
import com.boticavidasalud.backend.enums.TipoPresentacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioResponseDTO {

    private Integer id;

    private LocalDateTime fecha;

    private TipoMovimiento tipoMovimiento;

    // Producto
    private Integer idProducto;
    private String productoNombre;

    // Presentación
    private Integer idPresentacion;
    private TipoPresentacion tipoPresentacion;
    private Integer factorConversion;

    // Lote
    private Integer idLote;
    private String numeroLote;

    // Usuario responsable
    private Integer idUsuario;
    private String usuarioUsername;
    private String usuarioNombre;

    // Existencias expresadas en unidad base
    private BigDecimal stockAnterior;
    private BigDecimal cantidad;
    private BigDecimal stockNuevo;

    // Variación comercial de la presentación
    private BigDecimal cantidadPresentacion;

    // Motivo del movimiento
    private String motivo;
}


