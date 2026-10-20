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

    // Solo el administrador puede gestionar promociones
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DescuentoResponseDTO> crearDescuento(@Valid @RequestBody DescuentoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(descuentoService.crearDescuento(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DescuentoResponseDTO>> listarDescuentos() {
        return ResponseEntity.ok(descuentoService.listarDescuentos());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DescuentoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(descuentoService.obtenerPorId(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DescuentoResponseDTO> actualizarDescuento(
            @PathVariable Integer id, 
            @Valid @RequestBody DescuentoUpdateDTO request) {
        return ResponseEntity.ok(descuentoService.actualizarDescuento(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DescuentoResponseDTO> cambiarEstado(
            @PathVariable Integer id, 
            @RequestParam Short estado) {
        return ResponseEntity.ok(descuentoService.cambiarEstado(id, estado));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDescuento(@PathVariable Integer id) {
        descuentoService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


