package com.boticavidasalud.backend.security;

import com.boticavidasalud.backend.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
        // 1. Agregamos los permisos de la BD (ej. CLIENTE_LEER)
        List<GrantedAuthority> authorities = usuario.getPerfil().getOpciones().stream()
                .map(opcion -> new SimpleGrantedAuthority(opcion.getNombre()))
                .collect(Collectors.toList());
                
        // 2. Agregamos el rol clásico (ej. ROLE_ADMIN)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().getNombre()));
        return authorities;
    }

    @Override
    public String getPassword() { return usuario.getClave(); }
    @Override
    public String getUsername() { return usuario.getUsername(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return usuario.getEstado() == 1; }
}


