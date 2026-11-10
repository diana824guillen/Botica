package com.boticavidasalud.backend.security;

import com.boticavidasalud.backend.entity.Perfil;
import com.boticavidasalud.backend.entity.Usuario;
import com.boticavidasalud.backend.enums.EstadoRegistro;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private static final Short ESTADO_ACTIVO =
            EstadoRegistro.ACTIVO.getValor();

    private final Usuario usuario;
    private final List<String> permisos;

    public UserDetailsImpl(
            Usuario usuario,
            List<String> permisos) {

        this.usuario = usuario;

        this.permisos = permisos == null
                ? List.of()
                : List.copyOf(permisos);
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Agregar los permisos efectivos del usuario.
        for (String permiso : permisos) {

            if (permiso != null && !permiso.isBlank()) {
                authorities.add(
                        new SimpleGrantedAuthority(permiso)
                );
            }
        }

        // 2. Conservar el rol correspondiente al perfil.
        Perfil perfil = usuario.getPerfil();

        if (perfil != null) {

            String nombrePerfil = perfil.getNombre();

            if (nombrePerfil != null && !nombrePerfil.isBlank()) {

                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + nombrePerfil
                        )
                );
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getClave();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {

        // El usuario debe estar activo.
        if (!ESTADO_ACTIVO.equals(usuario.getEstado())) {
            return false;
        }

        // También debe pertenecer a un perfil activo.
        Perfil perfil = usuario.getPerfil();

        return perfil != null
                && ESTADO_ACTIVO.equals(perfil.getEstado());
    }
}


