package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.dto.ClienteUpdateDTO;
import com.boticavidasalud.backend.service.ClienteService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ==================== LEER ====================
    @PreAuthorize("hasAuthority('CLIENTE_LEER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listarClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(clienteService.listarClientesPaginado(page, size, search));
    }

    @PreAuthorize("hasAuthority('CLIENTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/{numeroDocumento}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(clienteService.obtenerPorDocumento(numeroDocumento));
    }

    @PreAuthorize("hasAuthority('CLIENTE_LEER') or hasRole('ADMIN')")
    @GetMapping("/id/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // ==================== CREAR ====================
    @PreAuthorize("hasAuthority('CLIENTE_CREAR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrarCliente(@Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== EDITAR ====================
    @PreAuthorize("hasAuthority('CLIENTE_EDITAR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteUpdateDTO request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @PreAuthorize("hasAuthority('CLIENTE_EDITAR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ClienteResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(clienteService.cambiarEstado(id, estado));
    }

    // ==================== ELIMINAR ====================
    @PreAuthorize("hasAuthority('CLIENTE_ELIMINAR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


