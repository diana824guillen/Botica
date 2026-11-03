package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.LotePresentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LotePresentacionRepository
        extends JpaRepository<LotePresentacion, Integer> {

    // Consultar las existencias por presentación dentro de un lote.
    List<LotePresentacion> findByLote_IdOrderByIdAsc(
            Integer idLote
    );

    // Buscar el saldo de una presentación específica dentro de un lote.
    Optional<LotePresentacion> findByLote_IdAndPresentacion_Id(
            Integer idLote,
            Integer idPresentacion
    );

    // Saber si una presentación ya tiene registros de inventario.
    // Se conservan incluso cuando su saldo llega a cero.
    boolean existsByPresentacion_Id(
            Integer idPresentacion
    );

    // Sumar únicamente las existencias que pueden venderse.
    // No considera lotes inactivos, vacíos o vencidos.
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
}


