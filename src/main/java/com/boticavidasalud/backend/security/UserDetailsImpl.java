package com.boticavidasalud.backend.security;

import com.boticavidasalud.backend.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Usuario usuario;

    public UserDetailsImpl(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Como solo tenemos un rol (ADMIN), lo devolvemos como autoridad
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().getNombre())
        );
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
        // Si bloqueoHasta es null o ya pasó, está desbloqueado
        if (usuario.getBloqueoHasta() == null) return true;
        return java.time.LocalDateTime.now().isAfter(usuario.getBloqueoHasta());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.getEstado() == 1;
    }
}


