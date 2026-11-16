package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.DashboardVentasResponseDTO;

import com.boticavidasalud.backend.service.DashboardService;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
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

}


