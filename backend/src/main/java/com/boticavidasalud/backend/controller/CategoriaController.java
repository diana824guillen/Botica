package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.CategoriaRequestDTO;
import com.boticavidasalud.backend.dto.CategoriaResponseDTO;
import com.boticavidasalud.backend.dto.CategoriaUpdateDTO;
import com.boticavidasalud.backend.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Solo ADMIN puede crear
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crearCategoria(
            @Valid @RequestBody CategoriaRequestDTO request) {
        CategoriaResponseDTO response = categoriaService.crearCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ADMIN y VENDEDOR pueden ver
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.listarCategorias());
    }

    // ADMIN y VENDEDOR pueden ver
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    // Solo ADMIN puede actualizar
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> actualizarCategoria(
            @PathVariable Integer id,
            @Valid @RequestBody CategoriaUpdateDTO request) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, request));
    }

    // Solo ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CategoriaResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(categoriaService.cambiarEstado(id, estado));
    }

     // Solo ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        categoriaService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


