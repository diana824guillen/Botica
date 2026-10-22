package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcionRepository extends JpaRepository<Opcion, Integer> {
}
