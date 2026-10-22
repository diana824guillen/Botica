package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Buscar por número de documento (solo activos o inactivos)
    @Query("SELECT c FROM Cliente c WHERE c.numeroDocumento = :numeroDocumento AND c.estado IN (0, 1)")
    Optional<Cliente> findByNumeroDocumento(@Param("numeroDocumento") String numeroDocumento);

    // Listar solo clientes NO eliminados
    @Query("SELECT c FROM Cliente c WHERE c.estado IN (0, 1) ORDER BY c.id DESC")
    List<Cliente> listarActivos();

    // Buscar por ID (solo activos o inactivos)
    @Query("SELECT c FROM Cliente c WHERE c.id = :id AND c.estado IN (0, 1)")
    Optional<Cliente> findByIdActivo(@Param("id") Integer id);
}


