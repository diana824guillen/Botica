package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.UsuarioClaveRequestDTO;
import com.boticavidasalud.backend.dto.UsuarioRequestDTO;
import com.boticavidasalud.backend.dto.UsuarioResponseDTO;
import com.boticavidasalud.backend.dto.UsuarioUpdateDTO;

import com.boticavidasalud.backend.entity.Perfil;
import com.boticavidasalud.backend.entity.Usuario;

import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.enums.TipoDocumento;

import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;

import com.boticavidasalud.backend.repository.PerfilRepository;
import com.boticavidasalud.backend.repository.UsuarioRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class UsuarioService {

    private static final String PERFIL_ADMIN = "ADMIN";

    private static final Short ESTADO_ACTIVO =
            EstadoRegistro.ACTIVO.getValor();

    private static final Short ESTADO_INACTIVO =
            EstadoRegistro.INACTIVO.getValor();

    private static final Short ESTADO_ELIMINADO =
            EstadoRegistro.ELIMINADO.getValor();

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // LISTAR USUARIOS
    // ============================================================

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listarUsuarios(
            int pagina,
            int tamano,
            String search) {

        if (pagina < 0) {
            throw new BusinessException(
                    "La página no puede ser negativa"
            );
        }

        if (tamano < 1 || tamano > 100) {
            throw new BusinessException(
                    "El tamaño de página debe estar entre 1 y 100"
            );
        }

        String termino = search == null
                ? ""
                : search.trim();

        Pageable pageable = PageRequest.of(
                pagina,
                tamano,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return usuarioRepository
                .listarPaginadoYFiltrado(termino, pageable)
                .map(this::mapearUsuario);
    }

    // ============================================================
    // OBTENER USUARIO POR ID
    // ============================================================

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuario(Integer id) {

        Usuario usuario = obtenerUsuarioExistente(id);

        return mapearUsuario(usuario);
    }

    // ============================================================
    // REGISTRAR USUARIO
    // ============================================================

    public UsuarioResponseDTO registrarUsuario(
            UsuarioRequestDTO request) {

        String documento = normalizarDocumento(
                request.getNumeroDocumento()
        );

        String username = normalizarUsername(
                request.getUsername()
        );

        validarFormatoDocumento(
                request.getTipoDocumento(),
                documento
        );

        validarCorreo(request.getCorreo());

        // Verificar documento duplicado.
        if (usuarioRepository.existsByNumeroDocumento(documento)) {

            throw new BusinessException(
                    "Ya existe un usuario con ese documento"
            );
        }

        // Verificar nombre de acceso duplicado.
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {

            throw new BusinessException(
                    "Ya existe un usuario con ese nombre de acceso"
            );
        }

        // Solo se permiten perfiles activos que no sean ADMIN.
        Perfil perfil = obtenerPerfilAsignable(
                request.getPerfilId()
        );

        Usuario usuario = new Usuario();

        usuario.setPerfil(perfil);

        usuario.setTipoDocumento(request.getTipoDocumento());

        usuario.setNumeroDocumento(documento);

        usuario.setNombres(
                normalizarTexto(request.getNombres())
        );

        usuario.setApellidos(
                normalizarTexto(request.getApellidos())
        );

        usuario.setUsername(username);

        // La contraseña nunca se guarda en texto plano.
        usuario.setClave(
                passwordEncoder.encode(request.getClave())
        );

        usuario.setCorreo(
                normalizarCorreo(request.getCorreo())
        );

        usuario.setEstado(ESTADO_ACTIVO);

        usuario.setIntentosLogin(0);
        usuario.setBloqueoHasta(null);
        usuario.setUltimoIntento(null);

        Usuario guardado = usuarioRepository.save(usuario);

        return mapearUsuario(guardado);
    }

    // ============================================================
    // ACTUALIZAR USUARIO
    // ============================================================

    public UsuarioResponseDTO actualizarUsuario(
            Integer id,
            UsuarioUpdateDTO request) {

        Usuario usuario = obtenerUsuarioExistente(id);

        validarUsuarioNoProtegido(usuario);

        String documento = normalizarDocumento(
                request.getNumeroDocumento()
        );

        String username = normalizarUsername(
                request.getUsername()
        );

        validarFormatoDocumento(
                request.getTipoDocumento(),
                documento
        );

        validarCorreo(request.getCorreo());

        // Evitar documentos duplicados en otros usuarios.
        if (usuarioRepository.existsByNumeroDocumentoAndIdNot(
                documento,
                id
        )) {

            throw new BusinessException(
                    "Ya existe un usuario con ese documento"
            );
        }

        // Evitar nombres de acceso duplicados.
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(
                username,
                id
        )) {

            throw new BusinessException(
                    "Ya existe un usuario con ese nombre de acceso"
            );
        }

        // Verificar que el nuevo perfil pueda asignarse.
        Perfil perfil = obtenerPerfilAsignable(
                request.getPerfilId()
        );

        usuario.setPerfil(perfil);

        usuario.setTipoDocumento(request.getTipoDocumento());

        usuario.setNumeroDocumento(documento);

        usuario.setNombres(
                normalizarTexto(request.getNombres())
        );

        usuario.setApellidos(
                normalizarTexto(request.getApellidos())
        );

        usuario.setUsername(username);

        usuario.setCorreo(
                normalizarCorreo(request.getCorreo())
        );

        Usuario actualizado = usuarioRepository.save(usuario);

        return mapearUsuario(actualizado);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR USUARIO
    // ============================================================

    public UsuarioResponseDTO cambiarEstado(
            Integer id,
            Short nuevoEstado) {

        if (
                nuevoEstado == null ||
                (
                    !nuevoEstado.equals(ESTADO_ACTIVO) &&
                    !nuevoEstado.equals(ESTADO_INACTIVO)
                )
        ) {

            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)"
            );
        }

        Usuario usuario = obtenerUsuarioExistente(id);

        validarUsuarioNoProtegido(usuario);

        if (usuario.getEstado().equals(nuevoEstado)) {
            return mapearUsuario(usuario);
        }

        usuario.setEstado(nuevoEstado);

        if (nuevoEstado.equals(ESTADO_INACTIVO)) {

            usuario.setFechaDesactivacion(LocalDateTime.now());

        } else {

            usuario.setFechaDesactivacion(null);
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        return mapearUsuario(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================

    public void eliminarLogicamente(Integer id) {

        Usuario usuario = obtenerUsuarioExistente(id);

        validarUsuarioNoProtegido(usuario);

        usuario.setEstado(ESTADO_ELIMINADO);

        usuario.setFechaEliminacion(LocalDateTime.now());

        usuarioRepository.save(usuario);
    }

    // ============================================================
    // RESTABLECER CONTRASEÑA
    // ============================================================

    public UsuarioResponseDTO restablecerClave(
            Integer id,
            UsuarioClaveRequestDTO request) {

        Usuario usuario = obtenerUsuarioExistente(id);

        validarUsuarioNoProtegido(usuario);

        // Guardar siempre la contraseña cifrada con BCrypt.
        usuario.setClave(
                passwordEncoder.encode(request.getNuevaClave())
        );

        // Restablecer los intentos y el bloqueo de la cuenta.
        usuario.setIntentosLogin(0);
        usuario.setBloqueoHasta(null);
        usuario.setUltimoIntento(null);

        Usuario actualizado = usuarioRepository.save(usuario);

        return mapearUsuario(actualizado);
    }

    // ============================================================
    // OBTENER USUARIO EXISTENTE
    // ============================================================

    private Usuario obtenerUsuarioExistente(Integer id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con ID: " + id
                        )
                );

        if (ESTADO_ELIMINADO.equals(usuario.getEstado())) {

            throw new ResourceNotFoundException(
                    "El usuario no se encuentra disponible"
            );
        }

        return usuario;
    }

    // ============================================================
    // OBTENER PERFIL ASIGNABLE
    // ============================================================

    private Perfil obtenerPerfilAsignable(Integer perfilId) {

        Perfil perfil = perfilRepository.findById(perfilId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Perfil no encontrado con ID: " + perfilId
                        )
                );

        if (!ESTADO_ACTIVO.equals(perfil.getEstado())) {

            throw new BusinessException(
                    "No se puede asignar un perfil inactivo"
            );
        }

        if (esPerfilAdmin(perfil)) {

            throw new BusinessException(
                    "No se puede asignar el perfil ADMIN a otro usuario"
            );
        }

        return perfil;
    }

    // ============================================================
    // PROTEGER CUENTA ADMIN
    // ============================================================

    private void validarUsuarioNoProtegido(Usuario usuario) {

        if (esPerfilAdmin(usuario.getPerfil())) {

            throw new BusinessException(
                    "La cuenta ADMIN está protegida y no puede "
                    + "modificarse, desactivarse ni eliminarse"
            );
        }
    }

    private boolean esPerfilAdmin(Perfil perfil) {

        return perfil != null
                && perfil.getNombre() != null
                && PERFIL_ADMIN.equalsIgnoreCase(
                        perfil.getNombre().trim()
                );
    }

    // ============================================================
    // NORMALIZAR DOCUMENTO
    // ============================================================

    private String normalizarDocumento(String documento) {

        return documento == null
                ? ""
                : documento.trim().toUpperCase(Locale.ROOT);
    }

    // ============================================================
    // VALIDAR FORMATO DE DOCUMENTO
    // ============================================================

    private void validarFormatoDocumento(
            TipoDocumento tipo,
            String numero) {

        if (tipo == null || numero.isBlank()) {

            throw new BusinessException(
                    "El tipo y número de documento son obligatorios"
            );
        }

        boolean valido = switch (tipo) {

            case DNI ->
                    numero.matches("\\d{8}");

            case CE ->
                    numero.matches("[A-Z0-9]{9,11}");

            case PASAPORTE ->
                    numero.matches("[A-Z0-9]{9,15}");
        };

        if (!valido) {

            throw new BusinessException(
                    "El número de documento no tiene un formato válido para "
                    + tipo
            );
        }
    }

    // ============================================================
    // NORMALIZAR NOMBRE DE USUARIO
    // ============================================================

    private String normalizarUsername(String username) {

        return username == null
                ? ""
                : username.trim().toLowerCase(Locale.ROOT);
    }

    // ============================================================
    // NORMALIZAR TEXTO
    // ============================================================

    private String normalizarTexto(String valor) {

        return valor == null
                ? ""
                : valor.trim();
    }

    // ============================================================
    // NORMALIZAR CORREO
    // ============================================================

    private String normalizarCorreo(String correo) {

        if (correo == null || correo.isBlank()) {
            return null;
        }

        return correo.trim();
    }

    // ============================================================
    // VALIDAR CORREO
    // ============================================================

    private void validarCorreo(String correo) {

        if (correo == null || correo.isBlank()) {
            return;
        }

        String valor = correo.trim();

        if (!valor.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new BusinessException(
                    "El correo electrónico no tiene un formato válido"
            );
        }
    }

    // ============================================================
    // MAPEAR USUARIO A DTO
    // ============================================================

    private UsuarioResponseDTO mapearUsuario(Usuario usuario) {

        Perfil perfil = usuario.getPerfil();

        boolean bloqueado =
                usuario.getBloqueoHasta() != null
                && usuario.getBloqueoHasta().isAfter(
                        LocalDateTime.now()
                );

        boolean protegido = esPerfilAdmin(perfil);

        return new UsuarioResponseDTO(
                usuario.getId(),
                perfil != null ? perfil.getId() : null,
                perfil != null ? perfil.getNombre() : null,
                usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getUsername(),
                usuario.getCorreo(),
                usuario.getEstado(),
                usuario.getFechaCreacion(),
                bloqueado,
                usuario.getBloqueoHasta(),
                protegido
        );
    }
}


