package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.ProductoUpdateDTO;
import com.boticavidasalud.backend.dto.PresentacionDTO;
import com.boticavidasalud.backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO request) {
        ProductoResponseDTO response = productoService.crearProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LEER ====================
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @RequestParam(required = false) Integer categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(productoService.listarProductosPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoResponseDTO>> listarStockBajo() {
        return ResponseEntity.ok(productoService.listarProductosConStockBajo());
    }

    @GetMapping("/por-vencer")
    public ResponseEntity<List<ProductoResponseDTO>> listarPorVencer(
            @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(productoService.listarProductosConLotesPorVencer(dias));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<ProductoResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(productoService.listarProductosConLotesVencidos());
    }

    // ==================== ACTUALIZAR PRODUCTO ====================
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoUpdateDTO request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    // ==================== AGREGAR PRESENTACIONES ====================
    @PostMapping("/{id}/presentaciones")
    public ResponseEntity<ProductoResponseDTO> agregarPresentaciones(
            @PathVariable Integer id,
            @Valid @RequestBody List<PresentacionDTO> presentaciones) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.agregarPresentaciones(id, presentaciones));
    }

    // ==================== CAMBIAR ESTADO DE PRESENTACIÓN ====================
    @PatchMapping("/{id}/presentaciones/{idPresentacion}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstadoPresentacion(
            @PathVariable Integer id,
            @PathVariable Integer idPresentacion,
            @RequestParam Short estado) {
        return ResponseEntity.ok(productoService.cambiarEstadoPresentacion(id, idPresentacion, estado));
    }

    // ==================== CAMBIAR ESTADO DEL PRODUCTO ====================
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(productoService.cambiarEstado(id, estado));
    }

    // ==================== ELIMINACIÓN LÓGICA ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


