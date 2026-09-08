package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
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

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO request) {
        ProductoResponseDTO response = productoService.crearProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @RequestParam(required = false) Integer categoriaId) {
        if (categoriaId != null) {
            return ResponseEntity.ok(productoService.listarProductosPorCategoria(categoriaId));
        }
        return ResponseEntity.ok(productoService.listarProductos());
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
}

