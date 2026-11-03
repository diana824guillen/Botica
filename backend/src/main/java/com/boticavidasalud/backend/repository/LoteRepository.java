
package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Lote;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    // ============================================================
    // CONSULTAS GENERALES
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado IN (0, 1)
        ORDER BY l.fechaIngreso DESC, l.id DESC
        """)
    List<Lote> listarTodos();

    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado IN (0, 1)
        ORDER BY l.fechaIngreso DESC, l.id DESC
        """)
    List<Lote> listarPorProducto(
            @Param("idProducto") Integer idProducto
    );

    @Query("""
        SELECT l FROM Lote l
        WHERE l.id = :id
          AND l.estado IN (0, 1)
        """)
    Optional<Lote> findByIdActivo(
            @Param("id") Integer id
    );

    // ============================================================
    // BLOQUEO DE LOTE PARA OPERACIONES DE INVENTARIO
    // ============================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM Lote l
        WHERE l.id = :id
        """)
    Optional<Lote> findByIdForUpdate(
            @Param("id") Integer id
    );

    // ============================================================
    // VALIDACIÓN DE NÚMERO DE LOTE
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND LOWER(TRIM(l.numeroLote)) = LOWER(TRIM(:numeroLote))
          AND l.estado IN (0, 1)
        """)
    Optional<Lote> findByProductoAndNumeroLote(
            @Param("idProducto") Integer idProducto,
            @Param("numeroLote") String numeroLote
    );

    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND LOWER(TRIM(l.numeroLote)) = LOWER(TRIM(:numeroLote))
          AND l.id <> :id
          AND l.estado IN (0, 1)
        """)
    Optional<Lote> findByProductoAndNumeroLoteAndIdNot(
            @Param("idProducto") Integer idProducto,
            @Param("numeroLote") String numeroLote,
            @Param("id") Integer id
    );

    // ============================================================
    // CONSULTAS DE VENCIMIENTO
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento BETWEEN :hoy AND :limite
        ORDER BY l.fechaVencimiento ASC, l.fechaIngreso ASC
        """)
    List<Lote> listarPorVencer(
            @Param("hoy") LocalDate hoy,
            @Param("limite") LocalDate limite
    );

    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento < :hoy
        ORDER BY l.fechaVencimiento ASC, l.fechaIngreso ASC
        """)
    List<Lote> listarVencidos(
            @Param("hoy") LocalDate hoy
    );

    // ============================================================
    // LOTES ACTIVOS PARA VENTA
    // CRITERIO FEFO: PRIMERO VENCE, PRIMERO SALE
    // ============================================================

    // Consulta principal utilizada cuando se conoce la fecha actual.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento >= :hoy
        ORDER BY l.fechaVencimiento ASC,
                 l.fechaIngreso ASC,
                 l.id ASC
        """)
    List<Lote> findLotesActivosParaVenta(
            @Param("idProducto") Integer idProducto,
            @Param("hoy") LocalDate hoy
    );

    // Consulta de compatibilidad para llamadas que utilizan
    // la firma anterior de un solo parámetro.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento >= CURRENT_DATE
        ORDER BY l.fechaVencimiento ASC,
                 l.fechaIngreso ASC,
                 l.id ASC
        """)
    List<Lote> findLotesActivosParaVenta(
            @Param("idProducto") Integer idProducto
    );
}


