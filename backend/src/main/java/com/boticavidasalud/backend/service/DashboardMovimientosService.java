package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.MovimientoInventarioResponseDTO;

import com.boticavidasalud.backend.entity.Lote;
import com.boticavidasalud.backend.entity.MovimientoInventario;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.entity.Usuario;

import com.boticavidasalud.backend.repository.MovimientoInventarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class DashboardMovimientosService {

    private final MovimientoInventarioRepository movimientoRepository;


    public DashboardMovimientosService(
            MovimientoInventarioRepository movimientoRepository) {

        this.movimientoRepository = movimientoRepository;
    }


    // ============================================================
    // ÚLTIMOS CINCO MOVIMIENTOS
    // ============================================================

    public List<MovimientoInventarioResponseDTO>
            listarMovimientosRecientes() {

        Pageable pageable = PageRequest.of(
                0,
                5,
                Sort.by(
                        Sort.Order.desc("fecha"),
                        Sort.Order.desc("id")
                )
        );


        Page<MovimientoInventario> movimientos =
                movimientoRepository.buscarMovimientos(
                        null,
                        null,
                        null,
                        null,
                        pageable
                );


        return movimientos.getContent()
                .stream()
                .map(this::mapearMovimiento)
                .toList();
    }


    // ============================================================
    // MAPEAR MOVIMIENTO A DTO
    // ============================================================

    private MovimientoInventarioResponseDTO mapearMovimiento(
            MovimientoInventario movimiento) {

        MovimientoInventarioResponseDTO dto =
                new MovimientoInventarioResponseDTO();

        dto.setId(movimiento.getId());
        dto.setFecha(movimiento.getFecha());

        dto.setTipoMovimiento(
                movimiento.getTipoMovimiento()
        );


        // --------------------------------------------------------
        // LOTE
        // --------------------------------------------------------

        Lote lote = movimiento.getLote();

        if (lote != null) {

            dto.setIdLote(lote.getId());

            dto.setNumeroLote(
                    lote.getNumeroLote()
            );
        }


        // --------------------------------------------------------
        // PRESENTACIÓN
        // --------------------------------------------------------

        Presentacion presentacion =
                movimiento.getPresentacion();

        if (presentacion != null) {

            dto.setIdPresentacion(
                    presentacion.getId()
            );

            dto.setTipoPresentacion(
                    presentacion.getTipo()
            );

            dto.setFactorConversion(
                    presentacion.getFactorConversion()
            );
        }


        // --------------------------------------------------------
        // PRODUCTO
        // --------------------------------------------------------

        Producto producto = null;

        if (lote != null) {
            producto = lote.getProducto();
        }

        // Alternativa cuando el movimiento no tiene lote asociado.
        if (producto == null && presentacion != null) {
            producto = presentacion.getProducto();
        }

        if (producto != null) {

            dto.setIdProducto(producto.getId());

            dto.setProductoNombre(
                    producto.getNombre()
            );
        }


        // --------------------------------------------------------
        // USUARIO RESPONSABLE
        // --------------------------------------------------------

        Usuario usuario = movimiento.getUsuario();

        if (usuario != null) {

            dto.setIdUsuario(usuario.getId());

            dto.setUsuarioUsername(
                    usuario.getUsername()
            );

            String nombreCompleto = (
                    (usuario.getNombres() != null
                            ? usuario.getNombres()
                            : "")
                    + " "
                    + (usuario.getApellidos() != null
                            ? usuario.getApellidos()
                            : "")
            ).trim();

            dto.setUsuarioNombre(
                    nombreCompleto.isBlank()
                            ? usuario.getUsername()
                            : nombreCompleto
            );
        }


        // --------------------------------------------------------
        // EXISTENCIAS
        // --------------------------------------------------------

        dto.setStockAnterior(
                movimiento.getStockAnterior()
        );

        dto.setCantidad(
                movimiento.getCantidad()
        );

        dto.setCantidadPresentacion(
                movimiento.getCantidadPresentacion()
        );

        dto.setStockNuevo(
                movimiento.getStockNuevo()
        );


        // --------------------------------------------------------
        // MOTIVO
        // --------------------------------------------------------

        dto.setMotivo(
                movimiento.getMotivo()
        );

        return dto;
    }

}


