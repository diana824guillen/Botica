package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository
        extends JpaRepository<Categoria, Integer> {

    // Comprobar duplicados sin distinguir mayúsculas, minúsculas ni espacios en los extremos.
    @Query("SELECT c FROM Categoria c WHERE LOWER(TRIM(c.nombre)) = LOWER(TRIM(:nombre))")
    Optional<Categoria> findByNombre(@Param("nombre") String nombre);

    // Comprobar duplicados al actualizar, excluyendo la propia categoría.
    @Query("SELECT c FROM Categoria c WHERE LOWER(TRIM(c.nombre)) = LOWER(TRIM(:nombre)) AND c.id <> :id")
    Optional<Categoria> findByNombreAndIdNot(@Param("nombre") String nombre, @Param("id") Integer id);

    // Mostrar categorías activas e inactivas, pero no eliminadas.
    @Query("SELECT c FROM Categoria c WHERE c.estado IN (0, 1) ORDER BY c.id DESC")
    List<Categoria> listarActivas();

    // Obtener categorías que todavía no han sido eliminadas.
    @Query("SELECT c FROM Categoria c WHERE c.id = :id AND c.estado IN (0, 1)")
    Optional<Categoria> findByIdActivo(@Param("id") Integer id);
}


