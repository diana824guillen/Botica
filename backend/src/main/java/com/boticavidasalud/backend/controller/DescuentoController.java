package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.DescuentoRequestDTO;
import com.boticavidasalud.backend.dto.DescuentoResponseDTO;
import com.boticavidasalud.backend.dto.DescuentoUpdateDTO;
import com.boticavidasalud.backend.service.DescuentoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/descuentos")
public class DescuentoController {

    private final DescuentoService descuentoService;

    public DescuentoController(DescuentoService descuentoService) {
        this.descuentoService = descuentoService;
    }

    // ============================================================
    // CREAR DESCUENTO
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_CREAR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DescuentoResponseDTO> crearDescuento(
            @Valid @RequestBody DescuentoRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(descuentoService.crearDescuento(request));
    }

    // ============================================================
    // LISTAR DESCUENTOS
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_LEER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DescuentoResponseDTO>> listarDescuentos() {

        return ResponseEntity.ok(
                descuentoService.listarDescuentos()
        );
    }

    // ============================================================
    // OBTENER DESCUENTO POR ID
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_LEER') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DescuentoResponseDTO> obtenerPorId(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                descuentoService.obtenerPorId(id)
        );
    }

    // ============================================================
    // ACTUALIZAR DESCUENTO
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DescuentoResponseDTO> actualizarDescuento(
            @PathVariable Integer id,
            @Valid @RequestBody DescuentoUpdateDTO request) {

        return ResponseEntity.ok(
                descuentoService.actualizarDescuento(id, request)
        );
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR DESCUENTO
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DescuentoResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                descuentoService.cambiarEstado(id, estado)
        );
    }

    // ============================================================
    // ELIMINAR DESCUENTO
    // ============================================================

    @PreAuthorize("hasAuthority('DESCUENTO_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDescuento(
            @PathVariable Integer id) {

        descuentoService.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}


