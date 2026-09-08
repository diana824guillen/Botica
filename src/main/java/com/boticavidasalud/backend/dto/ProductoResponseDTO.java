package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponseDTO {
    private Integer id;
    private String categoriaNombre;
    private String nombre;
    private String descripcion;
    private Boolean requiereReceta;
    private BigDecimal stockMinimo;
    private BigDecimal stockActual;
    private Short estado;
    private LocalDateTime fechaCreacion;
    private Set<PresentacionInfoDTO> presentaciones;
}

