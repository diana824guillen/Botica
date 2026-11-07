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

    // Obtener únicamente la última venta para generar el correlativo.
    @Query("SELECT v FROM Venta v WHERE v.id = (SELECT MAX(v2.id) FROM Venta v2)")
    Optional<Venta> findUltimaVenta();

    // Historial de compras por cliente.
    @Query("SELECT v FROM Venta v WHERE v.cliente.id = :idCliente ORDER BY v.fechaVenta DESC")
    List<Venta> findByClienteId(
            @Param("idCliente") Integer idCliente
    );

    // =====================================================
    // HISTORIAL GENERAL DE VENTAS
    // Buscar por nota, nombre del cliente o documento. Permite filtrar por fechas y utilizar paginación.
    // =====================================================

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
}


