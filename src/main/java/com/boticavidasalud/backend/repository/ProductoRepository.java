package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Productos con stock total (suma de lotes) menor o igual al stock_minimo
    @Query("SELECT p FROM Producto p WHERE p.estado = 1 AND " +
           "(SELECT COALESCE(SUM(l.cantidad), 0) FROM Lote l WHERE l.producto = p AND l.estado = 1) <= p.stockMinimo")
    List<Producto> findProductosConStockBajo();

    // Productos por categoría (opcional)
    List<Producto> findByCategoriaId(Integer categoriaId);
}


