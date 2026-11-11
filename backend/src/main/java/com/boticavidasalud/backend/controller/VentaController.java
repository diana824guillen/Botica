
package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.HistorialCompraDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.VentaListadoDTO;
import com.boticavidasalud.backend.dto.VentaPrevisualizacionRequestDTO;
import com.boticavidasalud.backend.dto.VentaPrevisualizacionResponseDTO;
import com.boticavidasalud.backend.dto.VentaRequestDTO;
import com.boticavidasalud.backend.dto.VentaResponseDTO;
import com.boticavidasalud.backend.service.ProductoService;
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
    private final ProductoService productoService;

    public VentaController(
            VentaService ventaService,
            ProductoService productoService) {

        this.ventaService = ventaService;
        this.productoService = productoService;
    }

    // ============================================================
    // CATÁLOGO DE PRODUCTOS DISPONIBLES PARA EL PUNTO DE VENTA
    // No permite consultar el catálogo general de Productos.
    // ============================================================

    @PreAuthorize("hasAuthority('PUNTO_VENTA_ACCESO') or hasRole('ADMIN')")
    @GetMapping("/catalogo/productos")
    public ResponseEntity<List<ProductoResponseDTO>> listarProductosDisponibles() {

        return ResponseEntity.ok(
                productoService.listarProductosDisponiblesParaVenta()
        );
    }

    // ============================================================
    // PREVISUALIZAR VENTA
    // ============================================================

    @PreAuthorize("hasAuthority('PUNTO_VENTA_ACCESO') or hasRole('ADMIN')")
    @PostMapping("/previsualizacion")
    public ResponseEntity<VentaPrevisualizacionResponseDTO> previsualizarVenta(
            @Valid @RequestBody VentaPrevisualizacionRequestDTO request) {

        return ResponseEntity.ok(
                ventaService.previsualizarVenta(request)
        );
    }

    // ============================================================
    // REGISTRAR VENTA
    // También permite registrar un cliente nuevo dentro de la venta.
    // ============================================================

    @PreAuthorize("hasAuthority('PUNTO_VENTA_ACCESO') or hasRole('ADMIN')")
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

    @PreAuthorize("hasAuthority('HISTORIAL_VENTAS_ACCESO') or hasRole('ADMIN')")
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
    // DETALLE COMPLETO DE UNA VENTA
    // ============================================================

    @PreAuthorize("hasAuthority('HISTORIAL_VENTAS_ACCESO') or hasRole('ADMIN')")
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

    @PreAuthorize("hasAuthority('HISTORIAL_VENTAS_ACCESO') or hasRole('ADMIN')")
    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<HistorialCompraDTO>> obtenerHistorialCompras(
            @PathVariable Integer idCliente) {

        return ResponseEntity.ok(
                ventaService.obtenerHistorialCompras(idCliente)
        );
    }
}


