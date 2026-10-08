package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    // Buscar por nombre (solo activas o inactivas)
    @Query("SELECT c FROM Categoria c WHERE c.nombre = :nombre AND c.estado IN (0, 1)")
    Optional<Categoria> findByNombre(@Param("nombre") String nombre);

    // Buscar por nombre excluyendo un ID (para validar duplicado en UPDATE)
    @Query("SELECT c FROM Categoria c WHERE c.nombre = :nombre AND c.id <> :id AND c.estado IN (0, 1)")
    Optional<Categoria> findByNombreAndIdNot(@Param("nombre") String nombre, @Param("id") Integer id);

    // Listar solo categorías NO eliminadas
    @Query("SELECT c FROM Categoria c WHERE c.estado IN (0, 1) ORDER BY c.id DESC")
    List<Categoria> listarActivas();

    // Buscar por ID (solo activas o inactivas)
    @Query("SELECT c FROM Categoria c WHERE c.id = :id AND c.estado IN (0, 1)")
    Optional<Categoria> findByIdActivo(@Param("id") Integer id);
}


