package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PerfilRepository
        extends JpaRepository<Perfil, Integer> {

    // Buscar un perfil por nombre.
    Optional<Perfil> findByNombre(String nombre);

    // Buscar por nombre sin distinguir mayúsculas y minúsculas.
    Optional<Perfil> findByNombreIgnoreCase(String nombre);

    // Comprobar si ya existe un nombre de perfil.
    boolean existsByNombreIgnoreCase(String nombre);

    // Listar perfiles activos e inactivos, excluyendo los eliminados.
    List<Perfil> findByEstadoInOrderByIdDesc(
            Collection<Short> estados
    );

    // Obtener un perfil bloqueando su registro durante una operación crítica.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Perfil p
        WHERE p.id = :id
    """)
    Optional<Perfil> findByIdForUpdate(
            @Param("id") Integer id
    );
}


