package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.DashboardInventarioResponseDTO;
import com.boticavidasalud.backend.dto.DashboardVentasResponseDTO;
import com.boticavidasalud.backend.dto.MovimientoInventarioResponseDTO;

import com.boticavidasalud.backend.service.DashboardInventarioService;
import com.boticavidasalud.backend.service.DashboardMovimientosService;
import com.boticavidasalud.backend.service.DashboardService;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    private final DashboardInventarioService dashboardInventarioService;

    private final DashboardMovimientosService dashboardMovimientosService;


    public DashboardController(
            DashboardService dashboardService,
            DashboardInventarioService dashboardInventarioService,
            DashboardMovimientosService dashboardMovimientosService) {

        this.dashboardService = dashboardService;

        this.dashboardInventarioService = dashboardInventarioService;

        this.dashboardMovimientosService = dashboardMovimientosService;
    }


    // ============================================================
    // RESUMEN DE VENTAS
    // Requiere permiso de consulta del historial de ventas.
    // ============================================================

    @PreAuthorize(
        "hasAuthority('HISTORIAL_VENTAS_ACCESO') or hasRole('ADMIN')"
    )
    @GetMapping("/ventas")
    public ResponseEntity<DashboardVentasResponseDTO> obtenerResumenVentas(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desdeComparacion,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hastaComparacion) {

        return ResponseEntity.ok(
                dashboardService.obtenerResumenVentas(
                        desde,
                        hasta,
                        desdeComparacion,
                        hastaComparacion
                )
        );
    }


    // ============================================================
    // RESUMEN DE INVENTARIO
    // Requiere permiso de Productos o Lotes.
    // ============================================================

    @PreAuthorize(
        "hasAnyAuthority('PRODUCTO_ACCESO', 'LOTE_ACCESO') or hasRole('ADMIN')"
    )
    @GetMapping("/inventario")
    public ResponseEntity<DashboardInventarioResponseDTO>
            obtenerResumenInventario(

            @RequestParam(defaultValue = "30")
            int dias) {

        return ResponseEntity.ok(
                dashboardInventarioService.obtenerResumenInventario(dias)
        );
    }


    // ============================================================
    // ÚLTIMOS MOVIMIENTOS DE INVENTARIO
    // Requiere permiso de Movimientos de inventario.
    // ============================================================

    @PreAuthorize(
        "hasAuthority('MOVIMIENTO_INVENTARIO_ACCESO') or hasRole('ADMIN')"
    )
    @GetMapping("/movimientos-recientes")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>>
            listarMovimientosRecientes() {

        return ResponseEntity.ok(
                dashboardMovimientosService.listarMovimientosRecientes()
        );
    }

}


