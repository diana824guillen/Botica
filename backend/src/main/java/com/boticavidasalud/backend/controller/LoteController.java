package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.LoteAjusteDTO;
import com.boticavidasalud.backend.dto.LoteRequestDTO;
import com.boticavidasalud.backend.dto.LoteResponseDTO;
import com.boticavidasalud.backend.service.LoteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    // ============================================================
    // LISTAR TODOS LOS LOTES
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/lotes")
    public ResponseEntity<List<LoteResponseDTO>> listarLotes() {

        return ResponseEntity.ok(
                loteService.listarLotes()
        );
    }

    // ============================================================
    // CREAR LOTE
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_CREAR') or hasRole('ADMIN')")
    @PostMapping("/productos/{idProducto}/lotes")
    public ResponseEntity<LoteResponseDTO> crearLote(
            @PathVariable Integer idProducto,
            @Valid @RequestBody LoteRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loteService.crearLote(idProducto, request));
    }

    // ============================================================
    // LISTAR LOTES POR PRODUCTO
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/productos/{idProducto}/lotes")
    public ResponseEntity<List<LoteResponseDTO>> listarLotesPorProducto(
            @PathVariable Integer idProducto) {

        return ResponseEntity.ok(
                loteService.listarLotesPorProducto(idProducto)
        );
    }

    // ============================================================
    // OBTENER LOTE POR ID
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/lotes/{id}")
    public ResponseEntity<LoteResponseDTO> obtenerLote(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                loteService.obtenerLotePorId(id)
        );
    }

    // ============================================================
    // LOTES POR VENCER
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/lotes/por-vencer")
    public ResponseEntity<List<LoteResponseDTO>> listarLotesPorVencer(
            @RequestParam(defaultValue = "30") int dias) {

        return ResponseEntity.ok(
                loteService.listarLotesPorVencer(dias)
        );
    }

    // ============================================================
    // LOTES VENCIDOS
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/lotes/vencidos")
    public ResponseEntity<List<LoteResponseDTO>> listarLotesVencidos() {

        return ResponseEntity.ok(
                loteService.listarLotesVencidos()
        );
    }

    // ============================================================
    // AJUSTAR EXISTENCIAS
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/lotes/{id}/ajustar")
    public ResponseEntity<LoteResponseDTO> ajustarLote(
            @PathVariable Integer id,
            @Valid @RequestBody LoteAjusteDTO request) {

        return ResponseEntity.ok(
                loteService.ajustarLote(id, request)
        );
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR LOTE
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/lotes/{id}/estado")
    public ResponseEntity<LoteResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                loteService.cambiarEstado(id, estado)
        );
    }

    // ============================================================
    // ELIMINAR LOTE LÓGICAMENTE
    // ============================================================

    @PreAuthorize("hasAuthority('LOTE_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/lotes/{id}")
    public ResponseEntity<Void> eliminarLote(
            @PathVariable Integer id) {

        loteService.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}


