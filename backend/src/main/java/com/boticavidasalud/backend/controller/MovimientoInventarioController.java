package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.MovimientoInventarioResponseDTO;

import com.boticavidasalud.backend.enums.TipoMovimiento;

import com.boticavidasalud.backend.service.MovimientoInventarioService;

import org.springframework.data.domain.Page;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    public MovimientoInventarioController(
            MovimientoInventarioService movimientoService) {

        this.movimientoService = movimientoService;
    }

    // ============================================================
    // LISTAR MOVIMIENTOS DE INVENTARIO
    // ============================================================

    @PreAuthorize(
        "hasAuthority('MOVIMIENTO_INVENTARIO_ACCESO') or hasRole('ADMIN')"
    )
    @GetMapping
    public ResponseEntity<Page<MovimientoInventarioResponseDTO>>
            listarMovimientos(

            @RequestParam(required = false)
            String buscar,

            @RequestParam(required = false)
            TipoMovimiento tipo,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "10")
            int tamano) {

        return ResponseEntity.ok(
                movimientoService.listarMovimientos(
                        buscar,
                        tipo,
                        desde,
                        hasta,
                        pagina,
                        tamano
                )
        );
    }
}


