package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Integer> {

    // Lotes con fecha de vencimiento entre hoy y hoy + dias (próximos a vencer)
    @Query("SELECT l FROM Lote l WHERE l.estado = 1 AND l.fechaVencimiento BETWEEN :hoy AND :limite")
    List<Lote> findLotesPorVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    // Lotes vencidos (fecha anterior a hoy)
    @Query("SELECT l FROM Lote l WHERE l.estado = 1 AND l.fechaVencimiento < :hoy")
    List<Lote> findLotesVencidos(@Param("hoy") LocalDate hoy);
}


