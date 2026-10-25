package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Buscar por número de documento (solo activos o inactivos)
    @Query("SELECT c FROM Cliente c WHERE c.numeroDocumento = :numeroDocumento AND c.estado IN (0, 1)")
    Optional<Cliente> findByNumeroDocumento(@Param("numeroDocumento") String numeroDocumento);

    // Listar solo clientes NO eliminados (para el método antiguo)
    @Query("SELECT c FROM Cliente c WHERE c.estado IN (0, 1) ORDER BY c.id DESC")
    List<Cliente> listarActivos();

    // Buscar por ID (solo activos o inactivos)
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND c.estado IN (0, 1)")
    Optional<Cliente> findByIdActivo(@Param("id") Integer id);

    // Paginación y filtrado (NUEVO)
    @Query("SELECT c FROM Cliente c WHERE c.estado IN (0, 1) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(c.nombres) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR c.numeroDocumento LIKE CONCAT('%', :search, '%'))")
    Page<Cliente> listarPaginadoYFiltrado(@Param("search") String search, Pageable pageable);
}


