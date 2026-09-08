package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.client.MiApiCloudClient;
import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.entity.Cliente;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final MiApiCloudClient miApiCloudClient;

    public ClienteService(ClienteRepository clienteRepository, MiApiCloudClient miApiCloudClient) {
        this.clienteRepository = clienteRepository;
        this.miApiCloudClient = miApiCloudClient;
    }

    @Transactional
    public ClienteResponseDTO registrarCliente(ClienteRequestDTO request) {
        // 1. Validar si ya existe un cliente con ese número de documento
        Optional<Cliente> existente = clienteRepository.findByNumeroDocumento(request.getNumeroDocumento());
        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe un cliente con ese número de documento");
        }

        // 2. Determinar el tipo de documento
        TipoDocumento tipo = determinarTipoDocumento(request.getNumeroDocumento());

        // 3. Intentar autocompletar si es DNI
        ClienteResponseDTO datosApi = null;
        if (tipo == TipoDocumento.DNI) {
            datosApi = miApiCloudClient.consultarPorDni(request.getNumeroDocumento());
        }

        // 4. Construir la entidad Cliente
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(request.getNumeroDocumento());

        if (datosApi != null && datosApi.getNombres() != null) {
            // Usar datos de ApiCloud
            cliente.setNombres(datosApi.getNombres());
            cliente.setApellidos(datosApi.getApellidos());
            cliente.setCorreo(datosApi.getCorreo());
        } else {
            // Usar datos manuales (pueden ser null, pero nombres es obligatorio)
            cliente.setNombres(request.getNombres());
            cliente.setApellidos(request.getApellidos());
            cliente.setCorreo(request.getCorreo());
        }

        // Si aún no tiene nombres (porque ApiCloud falló y el request no los trajo)
        if (cliente.getNombres() == null || cliente.getNombres().isEmpty()) {
            throw new RuntimeException("Debe proporcionar al menos los nombres del cliente o el DNI debe ser válido en ApiCloud");
        }

        cliente.setEstado((short) 1);

        // 5. Guardar en BD
        Cliente guardado = clienteRepository.save(cliente);

        // 6. Mapear a DTO de respuesta
        return mapToResponse(guardado);
    }

    public ClienteResponseDTO obtenerPorDocumento(String numeroDocumento) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con documento: " + numeroDocumento));
        return mapToResponse(cliente);
    }

    public ClienteResponseDTO obtenerPorId(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return mapToResponse(cliente);
    }

    private TipoDocumento determinarTipoDocumento(String numero) {
        // Si tiene 8 dígitos → DNI
        if (numero.matches("\\d{8}")) {
            return TipoDocumento.DNI;
        }
        // Si empieza con C y 8 dígitos → CE (ej. C12345678)
        if (numero.matches("C\\d{8}")) {
            return TipoDocumento.CE;
        }
        // Si tiene letras y números → PASAPORTE
        if (numero.matches("[A-Za-z0-9]{6,12}")) {
            return TipoDocumento.PASAPORTE;
        }
        // Por defecto asumimos CE (puedes ajustar)
        return TipoDocumento.CE;
    }

    private ClienteResponseDTO mapToResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getCorreo(),
                cliente.getEstado()
        );
    }
}


