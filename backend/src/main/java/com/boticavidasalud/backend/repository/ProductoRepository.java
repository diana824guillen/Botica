package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // ============================================================
    // CONSULTAS JPQL
    // ============================================================

    // Listar solo productos NO eliminados
    @Query("SELECT p FROM Producto p WHERE p.estado IN (0, 1) ORDER BY p.id DESC")
    List<Producto> listarActivos();

    // Buscar por ID (solo activos o inactivos)
    @Query("SELECT p FROM Producto p WHERE p.id = :id AND p.estado IN (0, 1)")
    Optional<Producto> findByIdActivo(@Param("id") Integer id);

    // Verificar duplicado por nombre, ignorando mayúsculas y espacios en los extremos.
    @Query("SELECT p FROM Producto p WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre)) AND p.estado IN (0, 1)")
    Optional<Producto> findByNombre(@Param("nombre") String nombre);

    // Verificar duplicados al actualizar, excluyendo al mismo producto.
    @Query("SELECT p FROM Producto p WHERE LOWER(TRIM(p.nombre)) = LOWER(TRIM(:nombre)) AND p.id <> :id AND p.estado IN (0, 1)")
    Optional<Producto> findByNombreAndIdNot(@Param("nombre") String nombre, @Param("id") Integer id);

    // Filtrar por categoría (solo activos)
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.estado IN (0, 1) ORDER BY p.id DESC")
    List<Producto> listarPorCategoria(@Param("categoriaId") Integer categoriaId);

    // Productos con stock bajo (suma de lotes ≤ stock mínimo)
    @Query("SELECT p FROM Producto p WHERE p.estado = 1 AND " +
           "(SELECT COALESCE(SUM(l.cantidad), 0) FROM Lote l " +
           " WHERE l.producto = p AND l.estado = 1) <= p.stockMinimo")
    List<Producto> listarConStockBajo();

    // Productos con lotes por vencer entre hoy y hoy + días
    @Query("SELECT DISTINCT p FROM Producto p " +
           "JOIN p.lotes l " +
           "WHERE p.estado = 1 AND l.estado = 1 " +
           "AND l.fechaVencimiento BETWEEN :hoy AND :limite " +
           "ORDER BY p.id DESC")
    List<Producto> listarConLotesPorVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    // Productos con lotes vencidos
    @Query("SELECT DISTINCT p FROM Producto p " +
           "JOIN p.lotes l " +
           "WHERE p.estado = 1 AND l.estado = 1 " +
           "AND l.fechaVencimiento < :hoy " +
           "ORDER BY p.id DESC")
    List<Producto> listarConLotesVencidos(@Param("hoy") LocalDate hoy);
}


