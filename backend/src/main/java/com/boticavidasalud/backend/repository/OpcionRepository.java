package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Opcion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OpcionRepository
        extends JpaRepository<Opcion, Integer> {

    // Consultar únicamente las opciones activas.
    List<Opcion> findByEstadoOrderByRutaAscNombreAsc(
            Short estado
    );

    // Consultar opciones seleccionadas que también estén activas.
    List<Opcion> findByIdInAndEstadoOrderByRutaAscNombreAsc(
            Collection<Integer> ids,
            Short estado
    );
}


