package com.boticavidasalud.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardProductoFrecuenteDTO {

    private Integer idProducto;

    private String nombreProducto;

    // Cantidad de notas distintas donde aparece el producto.
    private Long cantidadNotas;
}


