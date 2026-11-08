package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcionResponseDTO {

    private Integer id;
    private String nombre;
    private String ruta;
    private String icono;
    private String modulo;
    private String accion;
    private Short estado;
}


