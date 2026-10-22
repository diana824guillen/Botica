package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.dto.ClienteUpdateDTO;
import com.boticavidasalud.backend.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ADMIN y VENDEDOR pueden registrar (durante venta)
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrarCliente(@Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Solo ADMIN puede listar todos los clientes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    // ADMIN y VENDEDOR pueden buscar por documento (útil en venta)
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/{numeroDocumento}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(clienteService.obtenerPorDocumento(numeroDocumento));
    }

    // ADMIN y VENDEDOR pueden buscar por ID
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @GetMapping("/id/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // Solo ADMIN puede actualizar
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteUpdateDTO request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    // Solo ADMIN puede cambiar estado
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ClienteResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(clienteService.cambiarEstado(id, estado));
    }

    // Solo ADMIN puede eliminar
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


