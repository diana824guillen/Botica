package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Presentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresentacionRepository extends JpaRepository<Presentacion, Integer> {
    @Query("SELECT p FROM Presentacion p WHERE p.producto.id = :productoId")
    List<Presentacion> findByProductoId(@Param("productoId") Integer productoId);
}


