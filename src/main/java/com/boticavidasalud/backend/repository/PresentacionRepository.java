package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Presentacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentacionRepository extends JpaRepository<Presentacion, Integer> {
    List<Presentacion> findByProductoId(Integer productoId);
}


