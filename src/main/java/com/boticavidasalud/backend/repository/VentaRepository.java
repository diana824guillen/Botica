package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    // Obtener la última venta para generar el correlativo
    @Query("SELECT v FROM Venta v ORDER BY v.id DESC")
    Optional<Venta> findUltimaVenta();
}


