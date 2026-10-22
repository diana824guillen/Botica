package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {
    Optional<Perfil> findByNombre(String nombre);
}


