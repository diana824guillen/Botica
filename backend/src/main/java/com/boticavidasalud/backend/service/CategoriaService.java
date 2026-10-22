package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.CategoriaRequestDTO;
import com.boticavidasalud.backend.dto.CategoriaResponseDTO;
import com.boticavidasalud.backend.dto.CategoriaUpdateDTO;
import com.boticavidasalud.backend.entity.Categoria;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // ============================================================
    // CREAR CATEGORÍA
    // ============================================================
    public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO request) {
        // Verificar duplicado
        categoriaRepository.findByNombre(request.getNombre()).ifPresent(c -> {
            throw new BusinessException("Ya existe una categoría con ese nombre");
        });

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setEstado(EstadoRegistro.ACTIVO.getValor());

        Categoria guardada = categoriaRepository.save(categoria);
        return mapToResponse(guardada);
    }

    // ============================================================
    // LISTAR CATEGORÍAS
    // ============================================================
    public List<CategoriaResponseDTO> listarCategorias() {
        return categoriaRepository.listarActivas().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================
    public CategoriaResponseDTO obtenerPorId(Integer id) {
        Categoria categoria = categoriaRepository.findByIdActivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        return mapToResponse(categoria);
    }

    // ============================================================
    // ACTUALIZAR CATEGORÍA
    // ============================================================
    public CategoriaResponseDTO actualizarCategoria(Integer id, CategoriaUpdateDTO request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoria.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede actualizar una categoría eliminada");
        }

        // Verificar duplicado en OTRA categoría
        categoriaRepository.findByNombreAndIdNot(request.getNombre(), id).ifPresent(c -> {
            throw new BusinessException("Ya existe otra categoría con ese nombre");
        });

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());

        Categoria actualizada = categoriaRepository.save(categoria);
        return mapToResponse(actualizada);
    }

    // ============================================================
    // CAMBIAR ESTADO
    // ============================================================
    public CategoriaResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoria.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede cambiar el estado de una categoría eliminada");
        }

        categoria.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            categoria.setFechaDesactivacion(LocalDateTime.now());
        } else {
            categoria.setFechaDesactivacion(null);
        }

        Categoria actualizada = categoriaRepository.save(categoria);
        return mapToResponse(actualizada);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================
    public void eliminarLogicamente(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoria.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("La categoría ya está eliminada");
        }

        categoria.setEstado(EstadoRegistro.ELIMINADO.getValor());
        categoria.setFechaEliminacion(LocalDateTime.now());
        categoriaRepository.save(categoria);
    }

    // ============================================================
    // AUXILIAR
    // ============================================================
    private CategoriaResponseDTO mapToResponse(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado()
        );
    }
}


