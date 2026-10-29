package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    // Listado general: los ingresos más recientes aparecen primero.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.estado IN (0, 1)
        ORDER BY l.fechaIngreso DESC, l.id DESC
        """)
    List<Lote> listarTodos();

    // Listado de lotes de un producto: más recientes primero.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado IN (0, 1)
        ORDER BY l.fechaIngreso DESC, l.id DESC
        """)
    List<Lote> listarPorProducto(
            @Param("idProducto") Integer idProducto
    );

    // Buscar un lote que no esté eliminado.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.id = :id
          AND l.estado IN (0, 1)
        """)
    Optional<Lote> findByIdActivo(
            @Param("id") Integer id
    );

    // Verificar si ya existe el número de lote para el producto,
    // ignorando diferencias de mayúsculas y espacios externos.
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

    // Comprobación de duplicidad al actualizar, excluyendo el mismo lote.
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

    // Lotes activos con existencias cuyo vencimiento está dentro del rango.
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

    // Lotes activos con existencias cuya fecha de vencimiento ya pasó.
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

    // IMPORTANTE:
    // Para las ventas se conserva el criterio de vencimiento más próximo.
    // No cambiar este orden por fecha de ingreso.
    @Query("""
        SELECT l FROM Lote l
        WHERE l.producto.id = :idProducto
          AND l.estado = 1
          AND l.cantidad > 0
        ORDER BY l.fechaVencimiento ASC, l.fechaIngreso ASC, l.id ASC
        """)
    List<Lote> findLotesActivosParaVenta(
            @Param("idProducto") Integer idProducto
    );
}


