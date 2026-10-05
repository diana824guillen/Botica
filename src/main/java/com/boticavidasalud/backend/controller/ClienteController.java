package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.dto.ClienteUpdateDTO;
import com.boticavidasalud.backend.service.ClienteProcedureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteProcedureService clienteService;

    public ClienteController(ClienteProcedureService clienteService) {
        this.clienteService = clienteService;
    }

    // ==================== CREAR ====================
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> registrarCliente(@Valid @RequestBody ClienteRequestDTO request) {
        ClienteResponseDTO response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LEER ====================
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @GetMapping("/{numeroDocumento}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(clienteService.obtenerPorDocumento(numeroDocumento));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // ==================== ACTUALIZAR ====================
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ClienteUpdateDTO request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    // ==================== CAMBIAR ESTADO ====================
    @PatchMapping("/{id}/estado")
    public ResponseEntity<ClienteResponseDTO> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Short estado) {
        return ResponseEntity.ok(clienteService.cambiarEstado(id, estado));
    }

    // ==================== ELIMINACIÓN LÓGICA ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarLogicamente(id);
        return ResponseEntity.noContent().build();
    }
}


