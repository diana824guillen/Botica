package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByNumeroDocumento(String numeroDocumento);
}
