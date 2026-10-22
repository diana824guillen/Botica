package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponseDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private Short estado;
}


