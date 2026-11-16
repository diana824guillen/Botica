package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.DashboardInventarioResponseDTO;
import com.boticavidasalud.backend.dto.DashboardLoteAlertaDTO;
import com.boticavidasalud.backend.dto.DashboardPresentacionAlertaDTO;

import com.boticavidasalud.backend.dto.LoteResponseDTO;
import com.boticavidasalud.backend.dto.LotePresentacionInfoDTO;
import com.boticavidasalud.backend.dto.PresentacionInfoDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;

import com.boticavidasalud.backend.enums.EstadoRegistro;

import com.boticavidasalud.backend.exception.BusinessException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Service
@Transactional(readOnly = true)
public class DashboardInventarioService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private static final Set<Integer> DIAS_PERMITIDOS =
            Set.of(7, 15, 30);

    private final ProductoService productoService;
    private final LoteService loteService;


    public DashboardInventarioService(
            ProductoService productoService,
            LoteService loteService) {

        this.productoService = productoService;
        this.loteService = loteService;
    }


    // ============================================================
    // RESUMEN GENERAL DE INVENTARIO
    // ============================================================

    public DashboardInventarioResponseDTO obtenerResumenInventario(
            int dias) {

        validarDiasAnticipacion(dias);

        LocalDate hoy = LocalDate.now();


        // --------------------------------------------------------
        // LISTAS DE PRESENTACIONES
        // --------------------------------------------------------

        List<DashboardPresentacionAlertaDTO> agotadas =
                new ArrayList<>();

        List<DashboardPresentacionAlertaDTO> porReponer =
                new ArrayList<>();


        // --------------------------------------------------------
        // CONSULTAR PRODUCTOS Y PRESENTACIONES
        // --------------------------------------------------------

        List<ProductoResponseDTO> productos =
                productoService.listarProductos();

        for (ProductoResponseDTO producto : productos) {

            // No incluir productos inactivos.
            if (!EstadoRegistro.ACTIVO.getValor().equals(
                    producto.getEstado())) {
                continue;
            }

            List<PresentacionInfoDTO> presentaciones =
                    producto.getPresentaciones();

            if (presentaciones == null) {
                continue;
            }

            for (PresentacionInfoDTO presentacion : presentaciones) {

                // No incluir presentaciones inactivas.
                if (!EstadoRegistro.ACTIVO.getValor().equals(
                        presentacion.getEstado())) {
                    continue;
                }

                BigDecimal stockDisponible =
                        presentacion.getStockDisponible() != null
                                ? presentacion.getStockDisponible()
                                : CERO;

                BigDecimal stockMinimo =
                        presentacion.getStockMinimo() != null
                                ? presentacion.getStockMinimo()
                                : CERO;


                DashboardPresentacionAlertaDTO alerta =
                        new DashboardPresentacionAlertaDTO(

                                producto.getId(),
                                producto.getNombre(),

                                presentacion.getId(),
                                presentacion.getTipo(),

                                presentacion.getFactorConversion(),

                                stockDisponible,
                                stockMinimo
                        );


                // ------------------------------------------------
                // PRESENTACIONES AGOTADAS
                // ------------------------------------------------

                if (stockDisponible.compareTo(CERO) <= 0) {

                    agotadas.add(alerta);

                    continue;
                }


                // ------------------------------------------------
                // PRESENTACIONES POR REPONER
                // ------------------------------------------------

                if (
                        stockMinimo.compareTo(CERO) > 0
                        && stockDisponible.compareTo(stockMinimo) <= 0
                ) {

                    porReponer.add(alerta);
                }
            }
        }


        // --------------------------------------------------------
        // ORDENAR PRESENTACIONES AGOTADAS
        // --------------------------------------------------------

        agotadas.sort((primero, segundo) -> {

            String nombrePrimero =
                    normalizarTexto(primero.getNombreProducto());

            String nombreSegundo =
                    normalizarTexto(segundo.getNombreProducto());

            int comparacionNombre =
                    nombrePrimero.compareToIgnoreCase(nombreSegundo);

            if (comparacionNombre != 0) {
                return comparacionNombre;
            }

            String tipoPrimero =
                    primero.getTipoPresentacion() != null
                            ? primero.getTipoPresentacion().name()
                            : "";

            String tipoSegundo =
                    segundo.getTipoPresentacion() != null
                            ? segundo.getTipoPresentacion().name()
                            : "";

            return tipoPrimero.compareTo(tipoSegundo);
        });


        // --------------------------------------------------------
        // ORDENAR PRESENTACIONES POR REPONER
        // --------------------------------------------------------

        porReponer.sort((primero, segundo) -> {

            BigDecimal stockPrimero =
                    obtenerStock(primero.getStockDisponible());

            BigDecimal stockSegundo =
                    obtenerStock(segundo.getStockDisponible());

            int comparacionStock =
                    stockPrimero.compareTo(stockSegundo);

            if (comparacionStock != 0) {
                return comparacionStock;
            }

            String nombrePrimero =
                    normalizarTexto(primero.getNombreProducto());

            String nombreSegundo =
                    normalizarTexto(segundo.getNombreProducto());

            return nombrePrimero.compareToIgnoreCase(nombreSegundo);
        });


        // --------------------------------------------------------
        // CONSULTAR LOTES VENCIDOS
        // --------------------------------------------------------

        List<DashboardLoteAlertaDTO> lotesVencidos =
                loteService.listarLotesVencidos()
                        .stream()
                        .map(lote -> mapearLoteAlerta(lote, hoy))
                        .toList();


        // --------------------------------------------------------
        // CONSULTAR LOTES PRÓXIMOS A VENCER
        // --------------------------------------------------------

        List<DashboardLoteAlertaDTO> lotesPorVencer =
                loteService.listarLotesPorVencer(dias)
                        .stream()
                        .map(lote -> mapearLoteAlerta(lote, hoy))
                        .toList();


        // --------------------------------------------------------
        // CONSTRUIR RESPUESTA
        // --------------------------------------------------------

        DashboardInventarioResponseDTO response =
                new DashboardInventarioResponseDTO();

        response.setDiasAnticipacion(dias);

        response.setCantidadPresentacionesAgotadas(
                (long) agotadas.size()
        );

        response.setCantidadPresentacionesPorReponer(
                (long) porReponer.size()
        );

        response.setCantidadLotesVencidos(
                (long) lotesVencidos.size()
        );

        response.setCantidadLotesProximosAVencer(
                (long) lotesPorVencer.size()
        );

        response.setPresentacionesAgotadas(agotadas);
        response.setPresentacionesPorReponer(porReponer);

        response.setLotesVencidos(lotesVencidos);
        response.setLotesProximosAVencer(lotesPorVencer);

        return response;
    }


    // ============================================================
    // VALIDAR DÍAS DE ANTICIPACIÓN
    // ============================================================

    private void validarDiasAnticipacion(int dias) {

        if (!DIAS_PERMITIDOS.contains(dias)) {

            throw new BusinessException(
                    "El periodo de anticipación debe ser de "
                    + "7, 15 o 30 días"
            );
        }
    }


    // ============================================================
    // MAPEAR LOTE A ALERTA DEL DASHBOARD
    // ============================================================

    private DashboardLoteAlertaDTO mapearLoteAlerta(
            LoteResponseDTO lote,
            LocalDate hoy) {

        LocalDate fechaVencimiento =
                lote.getFechaVencimiento();

        Long diasRestantes = fechaVencimiento != null
                ? ChronoUnit.DAYS.between(hoy, fechaVencimiento)
                : null;

        List<LotePresentacionInfoDTO> existencias =
                lote.getExistenciasPresentaciones() != null
                        ? lote.getExistenciasPresentaciones()
                        : List.of();

        return new DashboardLoteAlertaDTO(

                lote.getId(),

                lote.getIdProducto(),
                lote.getNombreProducto(),

                lote.getNumeroLote(),

                fechaVencimiento,

                diasRestantes,

                existencias
        );
    }


    // ============================================================
    // OBTENER STOCK
    // ============================================================

    private BigDecimal obtenerStock(BigDecimal stock) {

        return stock != null
                ? stock
                : CERO;
    }


    // ============================================================
    // NORMALIZAR TEXTO PARA ORDENAR
    // ============================================================

    private String normalizarTexto(String texto) {

        return texto != null
                ? texto
                : "";
    }

}


