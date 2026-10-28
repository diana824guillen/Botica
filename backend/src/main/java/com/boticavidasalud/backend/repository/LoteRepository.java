package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Lote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    // ============================================================
    // LISTAR TODOS LOS LOTES NO ELIMINADOS
    // ============================================================

    @Query("SELECT l FROM Lote l WHERE l.estado IN (0, 1) ORDER BY l.fechaVencimiento ASC, l.id DESC")
    List<Lote> listarTodos();

    // ============================================================
    // LISTAR LOTES POR PRODUCTO
    // ============================================================

    @Query("SELECT l FROM Lote l WHERE l.producto.id = :idProducto AND l.estado IN (0, 1) ORDER BY l.fechaVencimiento ASC")
    List<Lote> listarPorProducto(@Param("idProducto") Integer idProducto);

    // ============================================================
    // BUSCAR LOTE POR ID
    // ============================================================

    @Query("SELECT l FROM Lote l WHERE l.id = :id AND l.estado IN (0, 1)")
    Optional<Lote> findByIdActivo(@Param("id") Integer id);
    
    // ============================================================
    // VALIDAR NÚMERO DE LOTE DUPLICADO
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
    // LOTES PRÓXIMOS A VENCER
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento BETWEEN :hoy AND :limite
        ORDER BY l.fechaVencimiento ASC
        """)
    List<Lote> listarPorVencer(
            @Param("hoy") LocalDate hoy,
            @Param("limite") LocalDate limite
    );

    // ============================================================
    // LOTES VENCIDOS
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado = 1
          AND l.cantidad > 0
          AND l.fechaVencimiento < :hoy
        ORDER BY l.fechaVencimiento ASC
        """)
    List<Lote> listarVencidos(
            @Param("hoy") LocalDate hoy
    );

    // ============================================================
    // LOTES DISPONIBLES PARA VENTA
    // ============================================================

    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado = 1
          AND l.cantidad > 0
        ORDER BY l.fechaVencimiento ASC
        """)
    List<Lote> findLotesActivosParaVenta(
            @Param("idProducto") Integer idProducto
    );
}


