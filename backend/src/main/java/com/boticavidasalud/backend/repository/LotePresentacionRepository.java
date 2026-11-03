
package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.LotePresentacion;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LotePresentacionRepository
        extends JpaRepository<LotePresentacion, Integer> {

    // ============================================================
    // CONSULTAS GENERALES
    // ============================================================

    List<LotePresentacion> findByLote_IdOrderByIdAsc(
            Integer idLote
    );

    Optional<LotePresentacion> findByLote_IdAndPresentacion_Id(
            Integer idLote,
            Integer idPresentacion
    );

    boolean existsByPresentacion_Id(
            Integer idPresentacion
    );

    // ============================================================
    // STOCK DISPONIBLE DE UNA PRESENTACIÓN
    // ============================================================

    @Query("""
        SELECT COALESCE(SUM(lp.cantidad), 0)
        FROM LotePresentacion lp
        WHERE lp.presentacion.id = :idPresentacion
          AND lp.cantidad > 0
          AND lp.lote.estado = 1
          AND lp.lote.fechaVencimiento >= :hoy
        """)
    BigDecimal sumarStockDisponible(
            @Param("idPresentacion") Integer idPresentacion,
            @Param("hoy") LocalDate hoy
    );

    // ============================================================
    // LOTES QUE TIENEN EXISTENCIAS DE UNA PRESENTACIÓN
    // ============================================================

    @Query("""
        SELECT DISTINCT lp.lote.id
        FROM LotePresentacion lp
        WHERE lp.presentacion.id = :idPresentacion
          AND lp.cantidad > 0
          AND lp.lote.estado = 1
          AND lp.lote.fechaVencimiento >= :hoy
        """)
    List<Integer> findIdsLotesDisponiblesParaVenta(
            @Param("idPresentacion") Integer idPresentacion,
            @Param("hoy") LocalDate hoy
    );

    // ============================================================
    // SALDOS BLOQUEADOS PARA LA VENTA
    // Se utiliza después de bloquear los lotes correspondientes.
    // ============================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT lp FROM LotePresentacion lp
        WHERE lp.presentacion.id = :idPresentacion
          AND lp.lote.id IN :idsLotes
          AND lp.cantidad > 0
          AND lp.lote.estado = 1
          AND lp.lote.fechaVencimiento >= :hoy
        ORDER BY lp.lote.fechaVencimiento ASC,
                 lp.lote.fechaIngreso ASC,
                 lp.lote.id ASC
        """)
    List<LotePresentacion> findSaldosDisponiblesParaVentaForUpdate(
            @Param("idPresentacion") Integer idPresentacion,
            @Param("hoy") LocalDate hoy,
            @Param("idsLotes") List<Integer> idsLotes
    );
}


