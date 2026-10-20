package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    // Obtener la última venta para generar el correlativo
    @Query("SELECT v FROM Venta v ORDER BY v.id DESC")
    Optional<Venta> findUltimaVenta();

    // ==========================================
    // CONSULTAS JPQL PARA REPORTES
    // ==========================================

    // 1. Resumen de ventas por periodo
    @Query("SELECT COUNT(v), COALESCE(SUM(v.total), 0), COALESCE(SUM(v.descuentoTotal), 0) " +
           "FROM Venta v WHERE v.fechaVenta BETWEEN :inicio AND :fin AND v.estado = 1")
    List<Object[]> obtenerResumenVentas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // 2. Productos más vendidos
    @Query("SELECT p.nombre, SUM(d.cantidad) " +
           "FROM DetalleVenta d " +
           "JOIN d.presentacion pres " +
           "JOIN pres.producto p " +
           "JOIN d.venta v " +
           "WHERE v.fechaVenta BETWEEN :inicio AND :fin AND v.estado = 1 " +
           "GROUP BY p.id, p.nombre " +
           "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> obtenerProductosMasVendidos(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // 3. Clientes recurrentes (Solo ventas con cliente registrado)
    @Query("SELECT c.nombres, c.apellidos, COUNT(v.id), SUM(v.total) " +
           "FROM Venta v " +
           "JOIN v.cliente c " +
           "WHERE v.fechaVenta BETWEEN :inicio AND :fin AND v.estado = 1 " +
           "GROUP BY c.id, c.nombres, c.apellidos " +
           "ORDER BY SUM(v.total) DESC")
    List<Object[]> obtenerClientesRecurrentes(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}


