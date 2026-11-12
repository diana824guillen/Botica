
package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    // ============================================================
    // AUTENTICACIÓN
    // ============================================================

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

    // ============================================================
    // VALIDACIÓN DE DUPLICADOS
    // ============================================================

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(
            String username,
            Integer id
    );

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(
            String numeroDocumento,
            Integer id
    );

    // ============================================================
    // CONTAR USUARIOS ASOCIADOS A UN PERFIL
    // ============================================================

    long countByPerfil_Id(Integer perfilId);

    // ============================================================
    // LISTADO PAGINADO Y FILTRADO
    // Incluye usuarios activos e inactivos. Excluye los eliminados.
    // ============================================================

    @Query("""
        SELECT u
        FROM Usuario u
        WHERE u.estado IN (0, 1)
          AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(u.username) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(u.nombres) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR LOWER(u.apellidos) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
                OR u.numeroDocumento LIKE CONCAT(
                    '%', :search, '%'
                )
                OR LOWER(u.perfil.nombre) LIKE LOWER(
                    CONCAT('%', :search, '%')
                )
          )
    """)
    Page<Usuario> listarPaginadoYFiltrado(
            @Param("search") String search,
            Pageable pageable
    );
}


