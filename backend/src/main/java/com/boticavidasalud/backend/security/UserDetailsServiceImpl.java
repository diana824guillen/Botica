package com.boticavidasalud.backend.security;

import com.boticavidasalud.backend.entity.Opcion;
import com.boticavidasalud.backend.entity.Perfil;
import com.boticavidasalud.backend.entity.Usuario;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.repository.OpcionRepository;
import com.boticavidasalud.backend.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String PERFIL_ADMIN = "ADMIN";

    private static final Short ESTADO_ACTIVO =
            EstadoRegistro.ACTIVO.getValor();

    private final UsuarioRepository usuarioRepository;
    private final OpcionRepository opcionRepository;

    public UserDetailsServiceImpl(
            UsuarioRepository usuarioRepository,
            OpcionRepository opcionRepository) {

        this.usuarioRepository = usuarioRepository;
        this.opcionRepository = opcionRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario no encontrado: " + username
                        )
                );

        List<String> permisos = obtenerPermisosEfectivos(usuario);

        return new UserDetailsImpl(usuario, permisos);
    }

    // ============================================================
    // OBTENER PERMISOS EFECTIVOS DEL USUARIO
    // ============================================================

    private List<String> obtenerPermisosEfectivos(Usuario usuario) {

        Set<String> permisos = new LinkedHashSet<>();

        Perfil perfil = usuario.getPerfil();

        if (perfil == null) {
            return List.of();
        }

        String nombrePerfil = perfil.getNombre();

        boolean esAdmin = nombrePerfil != null
                && PERFIL_ADMIN.equalsIgnoreCase(nombrePerfil.trim());

        // ADMIN recibe todas las opciones activas de la BD.
        if (esAdmin) {

            List<Opcion> opcionesActivas =
                    opcionRepository.findByEstadoOrderByRutaAscNombreAsc(
                            ESTADO_ACTIVO
                    );

            for (Opcion opcion : opcionesActivas) {
                agregarPermisoActivo(permisos, opcion);
            }

        } else {

            // Los demás perfiles reciben únicamente sus opciones asignadas.
            Set<Opcion> opcionesAsignadas = perfil.getOpciones();

            if (opcionesAsignadas != null) {

                for (Opcion opcion : opcionesAsignadas) {
                    agregarPermisoActivo(permisos, opcion);
                }
            }
        }

        return List.copyOf(permisos);
    }

    // ============================================================
    // AGREGAR ÚNICAMENTE OPCIONES ACTIVAS
    // ============================================================

    private void agregarPermisoActivo(
            Set<String> permisos,
            Opcion opcion) {

        if (opcion == null) {
            return;
        }

        if (!ESTADO_ACTIVO.equals(opcion.getEstado())) {
            return;
        }

        String nombrePermiso = opcion.getNombre();

        if (nombrePermiso != null && !nombrePermiso.isBlank()) {
            permisos.add(nombrePermiso);
        }
    }
}


