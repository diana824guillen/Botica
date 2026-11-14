package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.MovimientoInventario;
import com.boticavidasalud.backend.enums.TipoMovimiento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Integer> {

    // ============================================================
    // HISTORIAL DE MOVIMIENTOS DE INVENTARIO
    // Permite buscar, filtrar por tipo y rango de fechas.
    // ============================================================

    @Query(
        value = """
            SELECT m
            FROM MovimientoInventario m

            LEFT JOIN m.lote l
            LEFT JOIN l.producto productoLote

            LEFT JOIN m.presentacion p
            LEFT JOIN p.producto productoPresentacion

            JOIN m.usuario u

            WHERE

                (:tipo IS NULL
                    OR m.tipoMovimiento = :tipo)

                AND

                (:desde IS NULL
                    OR m.fecha >= :desde)

                AND

                (:hasta IS NULL
                    OR m.fecha < :hasta)

                AND (

                    :buscar IS NULL

                    OR LOWER(COALESCE(productoLote.nombre, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(productoPresentacion.nombre, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(l.numeroLote, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(u.username, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        CONCAT(
                            COALESCE(u.nombres, ''),
                            ' ',
                            COALESCE(u.apellidos, '')
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(m.motivo, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                )

            """,

        countQuery = """
            SELECT COUNT(m)
            FROM MovimientoInventario m

            LEFT JOIN m.lote l
            LEFT JOIN l.producto productoLote

            LEFT JOIN m.presentacion p
            LEFT JOIN p.producto productoPresentacion

            JOIN m.usuario u

            WHERE

                (:tipo IS NULL
                    OR m.tipoMovimiento = :tipo)

                AND

                (:desde IS NULL
                    OR m.fecha >= :desde)

                AND

                (:hasta IS NULL
                    OR m.fecha < :hasta)

                AND (

                    :buscar IS NULL

                    OR LOWER(COALESCE(productoLote.nombre, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(productoPresentacion.nombre, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(l.numeroLote, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(u.username, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        CONCAT(
                            COALESCE(u.nombres, ''),
                            ' ',
                            COALESCE(u.apellidos, '')
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(COALESCE(m.motivo, ''))
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                )

            """
    )
    Page<MovimientoInventario> buscarMovimientos(

            @Param("buscar")
            String buscar,

            @Param("tipo")
            TipoMovimiento tipo,

            @Param("desde")
            LocalDateTime desde,

            @Param("hasta")
            LocalDateTime hasta,

            Pageable pageable
    );
}


