package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.client.MiApiCloudClient;
import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.dto.ClienteUpdateDTO;
import com.boticavidasalud.backend.entity.Cliente;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final MiApiCloudClient miApiCloudClient;

    public ClienteService(ClienteRepository clienteRepository, MiApiCloudClient miApiCloudClient) {
        this.clienteRepository = clienteRepository;
        this.miApiCloudClient = miApiCloudClient;
    }

    // ============================================================
    // CREAR CLIENTE (con fallback de MiAPI.Cloud)
    // ============================================================
    public ClienteResponseDTO registrarCliente(ClienteRequestDTO request) {
        String numeroDoc = request.getNumeroDocumento();

        // Verificar duplicado
        clienteRepository.findByNumeroDocumento(numeroDoc).ifPresent(c -> {
            throw new BusinessException("Ya existe un cliente con ese número de documento");
        });

        TipoDocumento tipo = determinarTipoDocumento(numeroDoc);
        String nombres = request.getNombres();
        String apellidos = request.getApellidos();
        String correo = request.getCorreo();

        // Si es DNI, intentar autocompletar con MiAPI.Cloud
        if (tipo == TipoDocumento.DNI) {
            ClienteResponseDTO datosApi = miApiCloudClient.consultarPorDni(numeroDoc);
            if (datosApi != null && datosApi.getNombres() != null) {
                nombres = datosApi.getNombres();
                apellidos = datosApi.getApellidos();
            } else {
                if (nombres == null || nombres.isEmpty()) {
                    throw new BusinessException(
                        "MiAPI.ClOUD no está disponible. Por favor, ingrese los nombres y apellidos manualmente."
                    );
                }
            }
        } else {
            if (nombres == null || nombres.isEmpty()) {
                throw new BusinessException(
                    "Para documentos distintos a DNI, los nombres son obligatorios."
                );
            }
        }

        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numeroDoc);
        cliente.setNombres(nombres);
        cliente.setApellidos(apellidos);
        cliente.setCorreo(correo);
        cliente.setEstado(EstadoRegistro.ACTIVO.getValor());

        Cliente guardado = clienteRepository.save(cliente);
        return mapToResponse(guardado);
    }

    // ============================================================
    // LISTAR CLIENTES (solo activos e inactivos)
    // ============================================================
    public List<ClienteResponseDTO> listarClientes() {
        return clienteRepository.listarActivos().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR DOCUMENTO
    // ============================================================
    public ClienteResponseDTO obtenerPorDocumento(String numeroDocumento) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con documento: " + numeroDocumento));
        return mapToResponse(cliente);
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================
    public ClienteResponseDTO obtenerPorId(Integer id) {
        Cliente cliente = clienteRepository.findByIdActivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return mapToResponse(cliente);
    }

    // ============================================================
    // ACTUALIZAR CLIENTE
    // ============================================================
    public ClienteResponseDTO actualizarCliente(Integer id, ClienteUpdateDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        if (cliente.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede actualizar un cliente eliminado");
        }

        cliente.setNombres(request.getNombres());
        cliente.setApellidos(request.getApellidos());
        cliente.setCorreo(request.getCorreo());

        Cliente actualizado = clienteRepository.save(cliente);
        return mapToResponse(actualizado);
    }

    // ============================================================
    // CAMBIAR ESTADO
    // ============================================================
    public ClienteResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        if (cliente.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede cambiar el estado de un cliente eliminado");
        }

        cliente.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            cliente.setFechaDesactivacion(LocalDateTime.now());
        } else {
            cliente.setFechaDesactivacion(null);
        }

        Cliente actualizado = clienteRepository.save(cliente);
        return mapToResponse(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================
    public void eliminarLogicamente(Integer id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        if (cliente.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("El cliente ya está eliminado");
        }

        cliente.setEstado(EstadoRegistro.ELIMINADO.getValor());
        cliente.setFechaEliminacion(LocalDateTime.now());
        clienteRepository.save(cliente);
    }

    // ============================================================
    // AUXILIARES
    // ============================================================
    private TipoDocumento determinarTipoDocumento(String numero) {
        if (numero.matches("\\d{8}")) return TipoDocumento.DNI;
        if (numero.matches("C\\d{8}")) return TipoDocumento.CE;
        if (numero.matches("[A-Za-z0-9]{6,12}")) return TipoDocumento.PASAPORTE;
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


