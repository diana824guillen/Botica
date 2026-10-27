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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;

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
        String numeroDoc = request.getNumeroDocumento() == null
                ? ""
                : request.getNumeroDocumento()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (numeroDoc.isBlank()) {
            throw new BusinessException(
                    "El número de documento es obligatorio"
            );
        }

        // 1. Comprobar duplicados antes de consultar MiApiCloud.
        if (clienteRepository.existsByNumeroDocumento(numeroDoc)) {
            throw new BusinessException(
                    "Ya existe un cliente con ese número de documento"
            );
        }

        // 2. Determinar el origen del registro.
        String origenDatos = request.getOrigenDatos() == null
                ? "MANUAL"
                : request.getOrigenDatos()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!"RENIEC".equals(origenDatos)
                && !"MANUAL".equals(origenDatos)) {

            throw new BusinessException(
                    "El origen del registro no es válido"
            );
        }

        // 3. Obtener y validar el tipo de documento.
        TipoDocumento tipo = request.getTipoDocumento() != null
                ? request.getTipoDocumento()
                : determinarTipoDocumento(numeroDoc);

        validarFormatoDocumento(tipo, numeroDoc);

        String nombres = limpiarTexto(request.getNombres());
        String apellidos = limpiarTexto(request.getApellidos());

        String correo = request.getCorreo() == null
                ? null
                : request.getCorreo().trim();

        // 4. Consultar MiApiCloud ÚNICAMENTE en modo RENIEC.
        if ("RENIEC".equals(origenDatos)) {

            if (tipo != TipoDocumento.DNI) {
                throw new BusinessException(
                        "La consulta de RENIEC está disponible solo para DNI. "
                        + "Seleccione Registro Manual."
                );
            }

            ClienteResponseDTO datosApi =
                    miApiCloudClient.consultarPorDni(numeroDoc);

            if (datosApi == null
                    || datosApi.getNombres() == null
                    || datosApi.getNombres().isBlank()) {

                throw new BusinessException(
                        "No fue posible consultar MiApiCloud. "
                        + "Puede cambiar a Registro Manual e ingresar "
                        + "los datos del cliente."
                );
            }

            nombres = datosApi.getNombres().trim();
            apellidos = limpiarTexto(datosApi.getApellidos());

        } else {

            // 5. Registro manual: no realiza llamadas a MiApiCloud.
            if (nombres == null || nombres.isBlank()) {
                throw new BusinessException(
                        "Los nombres son obligatorios para el registro manual"
                );
            }

            if (apellidos == null || apellidos.isBlank()) {
                throw new BusinessException(
                        "Los apellidos son obligatorios para el registro manual"
                );
            }
        }

        // 6. Crear el cliente.
        Cliente cliente = new Cliente();

        cliente.setTipoDocumento(tipo);
        cliente.setNumeroDocumento(numeroDoc);
        cliente.setNombres(nombres);
        cliente.setApellidos(apellidos);

        cliente.setCorreo(
                correo == null || correo.isBlank()
                        ? null
                        : correo
        );

        cliente.setEstado(EstadoRegistro.ACTIVO.getValor());
        cliente.setOrigenDatos(origenDatos);

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
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
            "Cliente no encontrado con ID: " + id
        ));

        if (cliente.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede actualizar un cliente eliminado");
        }

        // Validación adicional del documento en el servidor.
        if (request.getNumeroDocumento() != null) {

            String solicitado = request.getNumeroDocumento().trim().toUpperCase(Locale.ROOT);

            String actual = cliente.getNumeroDocumento() == null ? "" : cliente.getNumeroDocumento().trim()
                    .toUpperCase(Locale.ROOT);

            if (!solicitado.equals(actual)) {

                if (clienteRepository.existsByNumeroDocumento(solicitado)) {
                    throw new BusinessException("Ya existe un cliente con ese número de documento");
                }

                throw new BusinessException("No se permite modificar el número de documento del cliente");
            }
        }

        if (request.getTipoDocumento() != null && request.getTipoDocumento() != cliente.getTipoDocumento()) {
            throw new BusinessException("No se permite modificar el tipo de documento del cliente");
        }

        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(limpiarTexto(request.getApellidos()));

        cliente.setCorreo(request.getCorreo() == null || request.getCorreo().isBlank() ? null : request.getCorreo().trim());

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
    // Paginacion y filtrado 
    // ============================================================

    public Page<ClienteResponseDTO> listarClientesPaginado(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Cliente> clientesPage = clienteRepository.listarPaginadoYFiltrado(search, pageable);
        return clientesPage.map(this::mapToResponse);
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

    private void validarFormatoDocumento(TipoDocumento tipo, String numero) {
        boolean valido = switch (tipo) {
            case DNI -> numero.matches("\\d{8}");
            case CE -> numero.matches("[A-Z0-9]{9,11}");
            case PASAPORTE -> numero.matches("[A-Z0-9]{9,15}");
        };

        if (!valido) {
            throw new BusinessException("El número de documento no tiene un formato válido para " + tipo);
        }
    }

    private String limpiarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private ClienteResponseDTO mapToResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getCorreo(),
                cliente.getEstado(),
                cliente.getOrigenDatos()
        );
    }
}


