
package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    // Obtener únicamente la última venta para generar el correlativo.
    @Query("SELECT v FROM Venta v WHERE v.id = (SELECT MAX(v2.id) FROM Venta v2)")
    Optional<Venta> findUltimaVenta();

    // Historial de compras por cliente.
    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :idCliente ORDER BY v.fechaVenta DESC")
    List<Venta> findByClienteId(@Param("idCliente") Integer idCliente);
}


