package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.DescuentoRequestDTO;
import com.boticavidasalud.backend.dto.DescuentoResponseDTO;
import com.boticavidasalud.backend.dto.DescuentoUpdateDTO;
import com.boticavidasalud.backend.entity.Descuento;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.DescuentoRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DescuentoService {

    private final DescuentoRepository descuentoRepository;
    private final ProductoRepository productoRepository;

    public DescuentoService(
            DescuentoRepository descuentoRepository,
            ProductoRepository productoRepository) {

        this.descuentoRepository = descuentoRepository;
        this.productoRepository = productoRepository;
    }

    // ============================================================
    // CREAR DESCUENTO
    // ============================================================

    public DescuentoResponseDTO crearDescuento(
            DescuentoRequestDTO request) {

        validarFechas(request.getFechaInicio(), request.getFechaFin());

        Producto producto = productoRepository
                .findByIdActivo(request.getIdProducto())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado o eliminado"
                        )
                );

        if (!EstadoRegistro.ACTIVO.getValor().equals(producto.getEstado())) {
            throw new BusinessException(
                    "Solo se pueden crear promociones para productos activos"
            );
        }

        Descuento descuento = new Descuento();

        descuento.setProducto(producto);
        descuento.setNombre(normalizarNombre(request.getNombre()));
        descuento.setTipoCondicion(request.getTipoCondicion());
        descuento.setValorCondicion(request.getValorCondicion());
        descuento.setPorcentaje(request.getPorcentaje());
        descuento.setFechaInicio(request.getFechaInicio());
        descuento.setFechaFin(request.getFechaFin());
        descuento.setEstado(EstadoRegistro.ACTIVO.getValor());

        Descuento guardado = descuentoRepository.save(descuento);

        return mapToResponse(guardado);
    }

    // ============================================================
    // LISTAR DESCUENTOS
    // ============================================================

    public List<DescuentoResponseDTO> listarDescuentos() {

        return descuentoRepository.listarActivos()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CONSULTAR POR ID
    // ============================================================

    public DescuentoResponseDTO obtenerPorId(Integer id) {

        Descuento descuento = descuentoRepository.findByIdActivo(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Descuento no encontrado con ID: " + id
                        )
                );

        return mapToResponse(descuento);
    }

    // ============================================================
    // ACTUALIZAR DESCUENTO
    // El producto asociado no se modifica en esta operación.
    // ============================================================

    public DescuentoResponseDTO actualizarDescuento(
            Integer id,
            DescuentoUpdateDTO request) {

        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Descuento no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                descuento.getEstado())) {

            throw new BusinessException(
                    "No se puede actualizar una promoción eliminada"
            );
        }

        validarFechas(request.getFechaInicio(), request.getFechaFin());

        descuento.setNombre(normalizarNombre(request.getNombre()));
        descuento.setTipoCondicion(request.getTipoCondicion());
        descuento.setValorCondicion(request.getValorCondicion());
        descuento.setPorcentaje(request.getPorcentaje());
        descuento.setFechaInicio(request.getFechaInicio());
        descuento.setFechaFin(request.getFechaFin());

        Descuento actualizado = descuentoRepository.save(descuento);

        return mapToResponse(actualizado);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR
    // ============================================================

    public DescuentoResponseDTO cambiarEstado(
            Integer id,
            Short nuevoEstado) {

        if (nuevoEstado == null
                || (nuevoEstado != 0 && nuevoEstado != 1)) {

            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)"
            );
        }

        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Descuento no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                descuento.getEstado())) {

            throw new BusinessException(
                    "No se puede cambiar el estado de una promoción eliminada"
            );
        }

        descuento.setEstado(nuevoEstado);

        if (EstadoRegistro.INACTIVO.getValor().equals(nuevoEstado)) {
            descuento.setFechaDesactivacion(LocalDateTime.now());
        } else {
            descuento.setFechaDesactivacion(null);
        }

        Descuento actualizado = descuentoRepository.save(descuento);

        return mapToResponse(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================

    public void eliminarLogicamente(Integer id) {

        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Descuento no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                descuento.getEstado())) {

            throw new BusinessException(
                    "La promoción ya está eliminada"
            );
        }

        descuento.setEstado(EstadoRegistro.ELIMINADO.getValor());
        descuento.setFechaEliminacion(LocalDateTime.now());

        descuentoRepository.save(descuento);
    }

    // ============================================================
    // VALIDAR VIGENCIA
    // ============================================================

    private void validarFechas(LocalDate inicio, LocalDate fin) {

        if (inicio != null && fin != null && fin.isBefore(inicio)) {

            throw new BusinessException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
    }

    // ============================================================
    // NORMALIZAR NOMBRE
    // ============================================================

    private String normalizarNombre(String nombre) {

        if (nombre == null) {
            return "";
        }

        return nombre.trim().replaceAll("\\s+", " ");
    }

    // ============================================================
    // MAPEAR RESPUESTA
    // ============================================================

    private DescuentoResponseDTO mapToResponse(Descuento descuento) {

        return new DescuentoResponseDTO(
                descuento.getId(),
                descuento.getProducto().getNombre(),
                descuento.getNombre(),
                descuento.getTipoCondicion(),
                descuento.getValorCondicion(),
                descuento.getPorcentaje(),
                descuento.getFechaInicio(),
                descuento.getFechaFin(),
                descuento.getEstado()
        );
    }
}


