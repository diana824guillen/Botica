package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.client.MiApiCloudClient;
import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.entity.Cliente;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MiApiCloudClient miApiCloudClient;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequestDTO requestDTO;
    private Cliente cliente;
    private ClienteResponseDTO apiResponse;

    @BeforeEach
    void setUp() {
        requestDTO = new ClienteRequestDTO();
        requestDTO.setNumeroDocumento("12345678");
        requestDTO.setNombres("Juan");
        requestDTO.setApellidos("Perez");

        cliente = new Cliente();
        cliente.setId(1);
        cliente.setNumeroDocumento("12345678");
        cliente.setNombres("Juan");
        cliente.setApellidos("Perez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setEstado((short) 1);

        apiResponse = new ClienteResponseDTO();
        apiResponse.setNumeroDocumento("12345678");
        apiResponse.setNombres("Juan");
        apiResponse.setApellidos("Perez");
        apiResponse.setTipoDocumento(TipoDocumento.DNI);
    }

    @Test
    void registrarCliente_conDniValido_deberiaGuardarCliente() {
        when(clienteRepository.findByNumeroDocumento("12345678")).thenReturn(Optional.empty());
        when(miApiCloudClient.consultarPorDni("12345678")).thenReturn(apiResponse);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteResponseDTO response = clienteService.registrarCliente(requestDTO);

        assertNotNull(response);
        assertEquals("12345678", response.getNumeroDocumento());
        assertEquals("Juan", response.getNombres());
        assertEquals("Perez", response.getApellidos());
        assertEquals(TipoDocumento.DNI, response.getTipoDocumento());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void registrarCliente_conDniYaExistente_deberiaLanzarExcepcion() {
        when(clienteRepository.findByNumeroDocumento("12345678")).thenReturn(Optional.of(cliente));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clienteService.registrarCliente(requestDTO));
        assertEquals("Ya existe un cliente con ese número de documento", exception.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrarCliente_conDniInvalidoYsinNombres_deberiaLanzarExcepcion() {
        requestDTO.setNombres(null);
        when(clienteRepository.findByNumeroDocumento("12345678")).thenReturn(Optional.empty());
        when(miApiCloudClient.consultarPorDni("12345678")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> clienteService.registrarCliente(requestDTO));
        assertEquals("Debe proporcionar al menos los nombres del cliente o el DNI debe ser válido en ApiCloud", exception.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void obtenerPorDocumento_conDniExistente_deberiaRetornarCliente() {
        when(clienteRepository.findByNumeroDocumento("12345678")).thenReturn(Optional.of(cliente));

        ClienteResponseDTO response = clienteService.obtenerPorDocumento("12345678");

        assertNotNull(response);
        assertEquals("12345678", response.getNumeroDocumento());
        assertEquals("Juan", response.getNombres());
    }

    @Test
    void obtenerPorDocumento_conDniInexistente_deberiaLanzarExcepcion() {
        when(clienteRepository.findByNumeroDocumento("99999999")).thenReturn(Optional.empty());

        assertThrows(com.boticavidasalud.backend.exception.ResourceNotFoundException.class,
                () -> clienteService.obtenerPorDocumento("99999999"));
    }
}


