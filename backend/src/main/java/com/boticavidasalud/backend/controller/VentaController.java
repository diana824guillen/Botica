package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.HistorialCompraDTO;
import com.boticavidasalud.backend.dto.VentaListadoDTO;
import com.boticavidasalud.backend.dto.VentaPrevisualizacionRequestDTO;
import com.boticavidasalud.backend.dto.VentaPrevisualizacionResponseDTO;
import com.boticavidasalud.backend.dto.VentaRequestDTO;
import com.boticavidasalud.backend.dto.VentaResponseDTO;
import com.boticavidasalud.backend.service.VentaService;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // ============================================================
    // PREVISUALIZAR VENTA
    // ============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @PostMapping("/previsualizacion")
    public ResponseEntity<VentaPrevisualizacionResponseDTO> previsualizarVenta(
            @Valid @RequestBody VentaPrevisualizacionRequestDTO request) {

        return ResponseEntity.ok(
                ventaService.previsualizarVenta(request)
        );
    }

    // ============================================================
    // REGISTRAR VENTA
    // ============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrarVenta(
            @Valid @RequestBody VentaRequestDTO request) {

        VentaResponseDTO response =
                ventaService.registrarVenta(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // HISTORIAL GENERAL DE VENTAS
    // ============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping
    public ResponseEntity<Page<VentaListadoDTO>> listarVentas(

            @RequestParam(required = false)
            String buscar,

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
                ventaService.listarVentas(
                        buscar,
                        desde,
                        hasta,
                        pagina,
                        tamano
                )
        );
    }

    // ============================================================
    // CONSULTAR UNA VENTA COMPLETA
    // ============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/{idVenta}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorId(
            @PathVariable Integer idVenta) {

        return ResponseEntity.ok(
                ventaService.obtenerVentaPorId(idVenta)
        );
    }

    // ============================================================
    // HISTORIAL DE COMPRAS DE UN CLIENTE
    // ============================================================

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<HistorialCompraDTO>> obtenerHistorialCompras(
            @PathVariable Integer idCliente) {

        return ResponseEntity.ok(
                ventaService.obtenerHistorialCompras(idCliente)
        );
    }
}


