package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.VentaRequestDTO;
import com.boticavidasalud.backend.dto.VentaResponseDTO;
import com.boticavidasalud.backend.service.VentaService;
import jakarta.validation.Valid;
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

    // Permitimos que tanto ADMIN como VENDEDOR registren ventas
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrarVenta(@Valid @RequestBody VentaRequestDTO request) {
        VentaResponseDTO response = ventaService.registrarVenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


