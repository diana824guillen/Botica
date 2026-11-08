package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponseDTO {

    private Integer id;
    private String nombre;
    private String descripcion;
    private Short estado;
    private long totalUsuarios;
    private int totalPermisos;
    private List<Integer> opcionIds;
    private boolean protegido;
}


