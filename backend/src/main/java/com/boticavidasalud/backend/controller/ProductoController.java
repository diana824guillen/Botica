package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.ProductoUpdateDTO;
import com.boticavidasalud.backend.dto.PresentacionDTO;
import com.boticavidasalud.backend.dto.PresentacionInfoDTO;
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

    // ==================== CREAR PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(
            @Valid @RequestBody ProductoRequestDTO request) {

        ProductoResponseDTO response =
                productoService.crearProducto(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LISTAR PRODUCTOS ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @RequestParam(required = false) Integer categoriaId) {

        if (categoriaId != null) {
            return ResponseEntity.ok(
                    productoService.listarProductosPorCategoria(categoriaId)
            );
        }

        return ResponseEntity.ok(productoService.listarProductos());
    }

    // ==================== OBTENER PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                productoService.obtenerProductoPorId(id)
        );
    }

    // ==================== LISTAR PRESENTACIONES DE UN PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/{id}/presentaciones")
    public ResponseEntity<List<PresentacionInfoDTO>> listarPresentaciones(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                productoService.listarPresentaciones(id)
        );
    }

    // ==================== AGREGAR PRESENTACIONES ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @PostMapping("/{id}/presentaciones")
    public ResponseEntity<ProductoResponseDTO> agregarPresentaciones(
            @PathVariable Integer id,
            @RequestBody List<@Valid PresentacionDTO> presentaciones) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                productoService.agregarPresentaciones(id, presentaciones)
        );
    }

    // ==================== ACTUALIZAR PRESENTACIÓN ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}/presentaciones/{idPresentacion}")
    public ResponseEntity<PresentacionInfoDTO> actualizarPresentacion(
            @PathVariable Integer id,
            @PathVariable Integer idPresentacion,
            @Valid @RequestBody PresentacionDTO request) {

        return ResponseEntity.ok(
                productoService.actualizarPresentacion(
                        id,
                        idPresentacion,
                        request
                )
        );
    }

    // ==================== ACTIVAR / DESACTIVAR PRESENTACIÓN ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/presentaciones/{idPresentacion}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstadoPresentacion(
            @PathVariable Integer id,
            @PathVariable Integer idPresentacion,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                productoService.cambiarEstadoPresentacion(
                        id,
                        idPresentacion,
                        estado
                )
        );
    }

    // ==================== ELIMINAR PRESENTACIÓN ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}/presentaciones/{idPresentacion}")
    public ResponseEntity<Void> eliminarPresentacion(
            @PathVariable Integer id,
            @PathVariable Integer idPresentacion) {

        productoService.eliminarPresentacion(id, idPresentacion);

        return ResponseEntity.noContent().build();
    }

    // ==================== STOCK BAJO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoResponseDTO>> listarStockBajo() {
        return ResponseEntity.ok(
                productoService.listarProductosConStockBajo()
        );
    }

    // ==================== LOTES POR VENCER ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/por-vencer")
    public ResponseEntity<List<ProductoResponseDTO>> listarPorVencer(
            @RequestParam(defaultValue = "30") int dias) {

        return ResponseEntity.ok(
                productoService.listarProductosConLotesPorVencer(dias)
        );
    }

    // ==================== LOTES VENCIDOS ====================

    @PreAuthorize("hasAuthority('PRODUCTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/vencidos")
    public ResponseEntity<List<ProductoResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(
                productoService.listarProductosConLotesVencidos()
        );
    }

    // ==================== ACTUALIZAR PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoUpdateDTO request) {

        return ResponseEntity.ok(
                productoService.actualizarProducto(id, request)
        );
    }

    // ==================== ACTIVAR / DESACTIVAR PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                productoService.cambiarEstado(id, estado)
        );
    }

    // ==================== ELIMINAR PRODUCTO ====================

    @PreAuthorize("hasAuthority('PRODUCTO_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Integer id) {

        productoService.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}


