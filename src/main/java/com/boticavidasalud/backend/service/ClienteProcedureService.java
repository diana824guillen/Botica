package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.client.MiApiCloudClient;
import com.boticavidasalud.backend.dto.ClienteRequestDTO;
import com.boticavidasalud.backend.dto.ClienteResponseDTO;
import com.boticavidasalud.backend.dto.ClienteUpdateDTO;
import com.boticavidasalud.backend.entity.Cliente;
import com.boticavidasalud.backend.enums.TipoDocumento;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClienteProcedureService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ClienteRepository clienteRepository;
    private final MiApiCloudClient miApiCloudClient;

    public ClienteProcedureService(ClienteRepository clienteRepository,
                                   MiApiCloudClient miApiCloudClient) {
        this.clienteRepository = clienteRepository;
        this.miApiCloudClient = miApiCloudClient;
    }

    // ============================================================
    // REGISTRAR CLIENTE CON LÓGICA DE FALLBACK
    // ============================================================
    public ClienteResponseDTO registrarCliente(ClienteRequestDTO request) {
        String numeroDoc = request.getNumeroDocumento();
        TipoDocumento tipo = determinarTipoDocumento(numeroDoc);

        String nombres = request.getNombres();
        String apellidos = request.getApellidos();
        String correo = request.getCorreo();

        // Si es DNI, intentar autocompletar con MiAPI.ClOUD
        if (tipo == TipoDocumento.DNI) {
            ClienteResponseDTO datosApi = miApiCloudClient.consultarPorDni(numeroDoc);

            if (datosApi != null && datosApi.getNombres() != null) {
                // MiAPI.ClOUD funcionó → usar sus datos
                nombres = datosApi.getNombres();
                apellidos = datosApi.getApellidos();
            } else {
                // MiAPI.ClOUD falló → verificar datos manuales
                if (nombres == null || nombres.isEmpty()) {
                    throw new RuntimeException(
                        "MiAPI.ClOUD no está disponible. Por favor, ingrese los nombres y apellidos manualmente."
                    );
                }
            }
        } else {
            // CE o Pasaporte → requiere datos manuales
            if (nombres == null || nombres.isEmpty()) {
                throw new RuntimeException(
                    "Para documentos distintos a DNI, los nombres son obligatorios."
                );
            }
        }

        return crearClienteProcedure(tipo.name(), numeroDoc, nombres, apellidos, correo);
    }

    // ============================================================
    // CREAR CLIENTE (PROCEDIMIENTO CON OUT PARAMS)
    // ============================================================
    private ClienteResponseDTO crearClienteProcedure(String tipoDoc, String numeroDoc,
                                                      String nombres, String apellidos, String correo) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_cliente_crear")
                .registerStoredProcedureParameter("p_tipo_documento", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_numero_documento", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_nombres", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_apellidos", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_correo", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_id_cliente", Integer.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT)
                .setParameter("p_tipo_documento", tipoDoc)
                .setParameter("p_numero_documento", numeroDoc)
                .setParameter("p_nombres", nombres)
                .setParameter("p_apellidos", apellidos)
                .setParameter("p_correo", correo);

        query.execute();

        Integer idCliente = (Integer) query.getOutputParameterValue("p_id_cliente");
        String mensaje = (String) query.getOutputParameterValue("p_mensaje");

        if (idCliente == null || idCliente <= 0) {
            throw new RuntimeException(mensaje != null ? mensaje : "Error al crear el cliente");
        }

        return obtenerPorId(idCliente);
    }

    // ============================================================
    // LISTAR CLIENTES
    // ============================================================
    public List<ClienteResponseDTO> listarClientes() {
        return clienteRepository.listarClientes().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR DOCUMENTO
    // ============================================================
    public ClienteResponseDTO obtenerPorDocumento(String numeroDocumento) {
        List<Cliente> resultados = clienteRepository.obtenerPorDocumento(numeroDocumento);
        if (resultados.isEmpty()) {
            throw new ResourceNotFoundException("Cliente no encontrado con documento: " + numeroDocumento);
        }
        return mapToResponse(resultados.get(0));
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================
    public ClienteResponseDTO obtenerPorId(Integer id) {
        List<Cliente> resultados = clienteRepository.obtenerPorIdProcedure(id);
        if (resultados.isEmpty()) {
            throw new ResourceNotFoundException("Cliente no encontrado con ID: " + id);
        }
        return mapToResponse(resultados.get(0));
    }

    // ============================================================
    // ACTUALIZAR CLIENTE
    // ============================================================
    public ClienteResponseDTO actualizarCliente(Integer id, ClienteUpdateDTO request) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_cliente_actualizar")
                .registerStoredProcedureParameter("p_id_cliente", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_nombres", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_apellidos", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_correo", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT)
                .setParameter("p_id_cliente", id)
                .setParameter("p_nombres", request.getNombres())
                .setParameter("p_apellidos", request.getApellidos())
                .setParameter("p_correo", request.getCorreo());

        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        if (mensaje != null && mensaje.toLowerCase().contains("no encontrado")) {
            throw new ResourceNotFoundException(mensaje);
        }

        return obtenerPorId(id);
    }

    // ============================================================
    // CAMBIAR ESTADO
    // ============================================================
    public ClienteResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_cliente_cambiar_estado")
                .registerStoredProcedureParameter("p_id_cliente", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_nuevo_estado", Short.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT)
                .setParameter("p_id_cliente", id)
                .setParameter("p_nuevo_estado", nuevoEstado);

        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        if (mensaje != null && mensaje.toLowerCase().contains("no encontrado")) {
            throw new ResourceNotFoundException(mensaje);
        }

        return obtenerPorId(id);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================
    public void eliminarLogicamente(Integer id) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_cliente_eliminar_logico")
                .registerStoredProcedureParameter("p_id_cliente", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT)
                .setParameter("p_id_cliente", id);

        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        if (mensaje != null && mensaje.toLowerCase().contains("no encontrado")) {
            throw new ResourceNotFoundException(mensaje);
        }
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


