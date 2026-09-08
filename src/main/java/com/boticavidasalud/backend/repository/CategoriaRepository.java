package com.boticavidasalud.backend.repository;

import com.boticavidasalud.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}


