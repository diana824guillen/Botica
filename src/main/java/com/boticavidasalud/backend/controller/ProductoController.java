package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.ProductoUpdateDTO;
import com.boticavidasalud.backend.dto.PresentacionDTO;
import com.boticavidasalud.backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // ==================== CREAR ====================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO request) {
        ProductoResponseDTO response = productoService.crearProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LEER ====================
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @RequestParam(required = false) Integer categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(productoService.listarProductosPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoResponseDTO>> listarStockBajo() {
        return ResponseEntity.ok(productoService.listarProductosConStockBajo());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/por-vencer")
    public ResponseEntity<List<ProductoResponseDTO>> listarPorVencer(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(productoService.listarProductosConLotesPorVencer(dias));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/vencidos")
    public ResponseEntity<List<ProductoResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(productoService.listarProductosConLotesVencidos());
    }

    // ==================== ACTUALIZAR PRODUCTO ====================
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoUpdateDTO request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    // ==================== AGREGAR PRESENTACIONES ====================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/presentaciones")
    public ResponseEntity<ProductoResponseDTO> agregarPresentaciones(
            @PathVariable Integer id,
            @Valid @RequestBody List<PresentacionDTO> presentaciones) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.agregarPresentaciones(id, presentaciones));
    }

    // ==================== CAMBIAR ESTADO DE PRESENTACIÓN ====================
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/presentaciones/{idPresentacion}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstadoPresentacion(
            @PathVariable Integer id,
            @PathVariable Integer idPresentacion,
            @RequestParam Short estado) {
        return ResponseEntity.ok(productoService.cambiarEstadoPresentacion(id, idPresentacion, estado));
    }

    // ==================== CAMBIAR ESTADO DEL PRODUCTO ====================
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(productoService.cambiarEstado(id, estado));
    }

    // ==================== ELIMINACIÓN LÓGICA ====================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


