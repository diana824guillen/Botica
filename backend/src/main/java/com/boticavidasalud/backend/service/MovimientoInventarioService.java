package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.MovimientoInventarioResponseDTO;

import com.boticavidasalud.backend.entity.Lote;
import com.boticavidasalud.backend.entity.MovimientoInventario;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.entity.Usuario;

import com.boticavidasalud.backend.enums.TipoMovimiento;

import com.boticavidasalud.backend.exception.BusinessException;

import com.boticavidasalud.backend.repository.MovimientoInventarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;

    public MovimientoInventarioService(
            MovimientoInventarioRepository movimientoRepository) {

        this.movimientoRepository = movimientoRepository;
    }

    // ============================================================
    // LISTAR MOVIMIENTOS DE INVENTARIO
    // ============================================================

    public Page<MovimientoInventarioResponseDTO> listarMovimientos(

            String buscar,
            TipoMovimiento tipo,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamano) {

        // --------------------------------------------------------
        // VALIDAR PÁGINA
        // --------------------------------------------------------

        if (pagina < 0) {
            throw new BusinessException(
                    "El número de página no puede ser negativo"
            );
        }

        // --------------------------------------------------------
        // VALIDAR TAMAÑO
        // --------------------------------------------------------

        if (tamano < 1 || tamano > 100) {
            throw new BusinessException(
                    "El tamaño de página debe estar entre 1 y 100"
            );
        }

        // --------------------------------------------------------
        // VALIDAR RANGO DE FECHAS
        // --------------------------------------------------------

        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {

            throw new BusinessException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        // --------------------------------------------------------
        // NORMALIZAR BÚSQUEDA
        // --------------------------------------------------------

        String criterioBusqueda = buscar != null
                ? buscar.trim()
                : null;

        if (criterioBusqueda != null
                && criterioBusqueda.isEmpty()) {

            criterioBusqueda = null;
        }

        // --------------------------------------------------------
        // CONVERTIR FECHAS
        // --------------------------------------------------------

        LocalDateTime fechaDesde = desde != null
                ? desde.atStartOfDay()
                : null;

        // Incluir todo el día de la fecha final.
        LocalDateTime fechaHasta = hasta != null
                ? hasta.plusDays(1).atStartOfDay()
                : null;

        // --------------------------------------------------------
        // PAGINACIÓN Y ORDENAMIENTO
        // --------------------------------------------------------

        Pageable pageable = PageRequest.of(
                pagina,
                tamano,
                Sort.by(
                        Sort.Order.desc("fecha"),
                        Sort.Order.desc("id")
                )
        );

        // --------------------------------------------------------
        // CONSULTAR MOVIMIENTOS
        // --------------------------------------------------------

        Page<MovimientoInventario> movimientos =
                movimientoRepository.buscarMovimientos(

                        criterioBusqueda,
                        tipo,
                        fechaDesde,
                        fechaHasta,
                        pageable
                );

        // --------------------------------------------------------
        // CONVERTIR A DTO
        // --------------------------------------------------------

        return movimientos.map(this::mapearMovimiento);
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

        // Alternativa por si el movimiento no tiene lote asociado.
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


