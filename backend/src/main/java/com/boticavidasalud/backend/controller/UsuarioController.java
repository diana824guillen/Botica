package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.UsuarioClaveRequestDTO;
import com.boticavidasalud.backend.dto.UsuarioRequestDTO;
import com.boticavidasalud.backend.dto.UsuarioResponseDTO;
import com.boticavidasalud.backend.dto.UsuarioUpdateDTO;

import com.boticavidasalud.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ============================================================
    // LISTAR USUARIOS
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {

        return ResponseEntity.ok(
                usuarioService.listarUsuarios(
                        page,
                        size,
                        search
                )
        );
    }

    // ============================================================
    // OBTENER USUARIO POR ID
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                usuarioService.obtenerUsuario(id)
        );
    }

    // ============================================================
    // REGISTRAR USUARIO
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(
            @Valid @RequestBody UsuarioRequestDTO request) {

        UsuarioResponseDTO response =
                usuarioService.registrarUsuario(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // ACTUALIZAR DATOS DEL USUARIO
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateDTO request) {

        return ResponseEntity.ok(
                usuarioService.actualizarUsuario(id, request)
        );
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR USUARIO
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {

        return ResponseEntity.ok(
                usuarioService.cambiarEstado(id, estado)
        );
    }

    // ============================================================
    // RESTABLECER CONTRASEÑA
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/clave")
    public ResponseEntity<UsuarioResponseDTO> restablecerClave(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioClaveRequestDTO request) {

        return ResponseEntity.ok(
                usuarioService.restablecerClave(id, request)
        );
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Integer id) {

        usuarioService.eliminarLogicamente(id);

        return ResponseEntity.noContent().build();
    }
}


