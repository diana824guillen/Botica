package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.OpcionResponseDTO;
import com.boticavidasalud.backend.dto.PerfilPermisosRequestDTO;
import com.boticavidasalud.backend.dto.PerfilRequestDTO;
import com.boticavidasalud.backend.dto.PerfilResponseDTO;
import com.boticavidasalud.backend.dto.PerfilUpdateDTO;

import com.boticavidasalud.backend.service.PerfilService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfiles")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // ============================================================
    // LISTAR PERFILES
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PerfilResponseDTO>> listarPerfiles() {

        return ResponseEntity.ok(
                perfilService.listarPerfiles()
        );
    }

    // ============================================================
    // LISTAR OPCIONES / PERMISOS DISPONIBLES
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/opciones")
    public ResponseEntity<List<OpcionResponseDTO>> listarOpciones() {

        return ResponseEntity.ok(
                perfilService.listarOpcionesDisponibles()
        );
    }

    // ============================================================
    // OBTENER PERFIL POR ID
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                perfilService.obtenerPerfilPorId(id)
        );
    }

    // ============================================================
    // CREAR PERFIL
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PerfilResponseDTO> crearPerfil(
            @Valid @RequestBody PerfilRequestDTO request) {

        PerfilResponseDTO response =
                perfilService.crearPerfil(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // ACTUALIZAR DATOS DEL PERFIL
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> actualizarPerfil(
            @PathVariable Integer id,
            @Valid @RequestBody PerfilUpdateDTO request) {

        return ResponseEntity.ok(
                perfilService.actualizarPerfil(id, request)
        );
    }

    // ============================================================
    // ACTUALIZAR PERMISOS DEL PERFIL
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/permisos")
    public ResponseEntity<PerfilResponseDTO> actualizarPermisos(
            @PathVariable Integer id,
            @Valid @RequestBody PerfilPermisosRequestDTO request) {

        return ResponseEntity.ok(
                perfilService.actualizarPermisos(id, request)
        );
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR PERFIL
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PerfilResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                perfilService.cambiarEstado(id, estado)
        );
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPerfil(
            @PathVariable Integer id) {

        perfilService.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}


