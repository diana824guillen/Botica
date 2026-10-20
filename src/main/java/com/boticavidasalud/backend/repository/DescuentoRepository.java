package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DescuentoRepository extends JpaRepository<Descuento, Integer> {

    // Buscar descuentos vigentes hoy
    @Query("SELECT d FROM Descuento d WHERE d.estado = 1 " +
           "AND (d.fechaInicio IS NULL OR d.fechaInicio <= :hoy) " +
           "AND (d.fechaFin IS NULL OR d.fechaFin >= :hoy)")
    List<Descuento> findDescuentosVigentes(@Param("hoy") LocalDate hoy);

    // Listar solo los no eliminados
    @Query("SELECT d FROM Descuento d WHERE d.estado IN (0, 1) ORDER BY d.id DESC")
    List<Descuento> listarActivos();

    // Buscar por ID solo si no está eliminado
    @Query("SELECT d FROM Descuento d WHERE d.id = :id AND d.estado IN (0, 1)")
    Optional<Descuento> findByIdActivo(@Param("id") Integer id);
}


