package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Venta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    // ============================================================
    // CONSULTAS GENERALES
    // ============================================================

    // Obtener únicamente la última venta para generar el correlativo.
    @Query("""
        SELECT v
        FROM Venta v
        WHERE v.id = (
            SELECT MAX(v2.id)
            FROM Venta v2
        )
        """)
    Optional<Venta> findUltimaVenta();

    // Historial de compras por cliente.
    @Query("""
        SELECT v
        FROM Venta v
        WHERE v.cliente.id = :idCliente
        ORDER BY v.fechaVenta DESC
        """)
    List<Venta> findByClienteId(
            @Param("idCliente") Integer idCliente
    );


    // ============================================================
    // HISTORIAL GENERAL DE VENTAS
    // ============================================================

    @Query(
        value = """
            SELECT v
            FROM Venta v
            LEFT JOIN v.cliente c
            WHERE
                (:desde IS NULL OR v.fechaVenta >= :desde)
                AND
                (:hasta IS NULL OR v.fechaVenta < :hasta)

                AND (
                    :buscar IS NULL

                    OR LOWER(v.numeroNotaVenta)
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        COALESCE(
                            v.clienteNombreSnapshot,
                            CONCAT(
                                COALESCE(c.nombres, ''),
                                ' ',
                                COALESCE(c.apellidos, '')
                            ),
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        COALESCE(
                            v.clienteNumeroDocumentoSnapshot,
                            c.numeroDocumento,
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))
                )
            """,

        countQuery = """
            SELECT COUNT(v)
            FROM Venta v
            LEFT JOIN v.cliente c
            WHERE
                (:desde IS NULL OR v.fechaVenta >= :desde)
                AND
                (:hasta IS NULL OR v.fechaVenta < :hasta)

                AND (
                    :buscar IS NULL

                    OR LOWER(v.numeroNotaVenta)
                        LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        COALESCE(
                            v.clienteNombreSnapshot,
                            CONCAT(
                                COALESCE(c.nombres, ''),
                                ' ',
                                COALESCE(c.apellidos, '')
                            ),
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))

                    OR LOWER(
                        COALESCE(
                            v.clienteNumeroDocumentoSnapshot,
                            c.numeroDocumento,
                            ''
                        )
                    ) LIKE LOWER(CONCAT('%', :buscar, '%'))
                )
            """
    )
    Page<Venta> buscarVentas(
            @Param("buscar") String buscar,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );


    // ============================================================
    // DASHBOARD - RESUMEN DE VENTAS
    // ============================================================

    @Query("""
        SELECT
            SUM(v.total),
            COUNT(v.id)
        FROM Venta v
        WHERE v.estado = 1
          AND v.fechaVenta >= :desde
          AND v.fechaVenta < :hasta
        """)
    Object[] obtenerResumenVentasDashboard(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );


    // ============================================================
    // DASHBOARD - EVOLUCIÓN DE VENTAS POR DÍA
    // ============================================================

    @Query("""
        SELECT
            YEAR(v.fechaVenta),
            MONTH(v.fechaVenta),
            DAY(v.fechaVenta),
            SUM(v.total),
            COUNT(v.id)
        FROM Venta v
        WHERE v.estado = 1
          AND v.fechaVenta >= :desde
          AND v.fechaVenta < :hasta
        GROUP BY
            YEAR(v.fechaVenta),
            MONTH(v.fechaVenta),
            DAY(v.fechaVenta)
        ORDER BY
            YEAR(v.fechaVenta),
            MONTH(v.fechaVenta),
            DAY(v.fechaVenta)
        """)
    List<Object[]> obtenerVentasAgrupadasPorDia(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );


    // ============================================================
    // DASHBOARD - MÉTODOS DE PAGO
    // ============================================================

    @Query("""
        SELECT
            p.metodoPago,
            SUM(p.monto),
            COUNT(p.id)
        FROM PagoVenta p
        JOIN p.venta v
        WHERE p.estado = 1
          AND v.estado = 1
          AND v.fechaVenta >= :desde
          AND v.fechaVenta < :hasta
        GROUP BY p.metodoPago
        ORDER BY p.metodoPago
        """)
    List<Object[]> obtenerResumenMetodosPago(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );


    // ============================================================
    // DASHBOARD - PRODUCTOS MÁS SOLICITADOS
    // Cuenta las notas distintas donde aparece cada producto.
    // Evita duplicaciones cuando una venta utiliza varios lotes.
    // ============================================================

    @Query("""
        SELECT
            producto.id,
            producto.nombre,
            COUNT(DISTINCT v.id)
        FROM DetalleVenta d
        JOIN d.venta v
        JOIN d.presentacion presentacion
        JOIN presentacion.producto producto
        WHERE v.estado = 1
          AND d.estado = 1
          AND v.fechaVenta >= :desde
          AND v.fechaVenta < :hasta
        GROUP BY
            producto.id,
            producto.nombre
        ORDER BY
            COUNT(DISTINCT v.id) DESC,
            producto.nombre ASC
        """)
    List<Object[]> obtenerProductosMasSolicitadosDashboard(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

}


