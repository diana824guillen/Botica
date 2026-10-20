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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DescuentoService {

    private final DescuentoRepository descuentoRepository;
    private final ProductoRepository productoRepository;

    public DescuentoService(DescuentoRepository descuentoRepository, ProductoRepository productoRepository) {
        this.descuentoRepository = descuentoRepository;
        this.productoRepository = productoRepository;
    }

    public DescuentoResponseDTO crearDescuento(DescuentoRequestDTO request) {
        Producto producto = productoRepository.findByIdActivo(request.getIdProducto())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o eliminado"));

        validarFechas(request.getFechaInicio(), request.getFechaFin());

        Descuento descuento = new Descuento();
        descuento.setProducto(producto);
        descuento.setNombre(request.getNombre());
        descuento.setTipoCondicion(request.getTipoCondicion());
        descuento.setValorCondicion(request.getValorCondicion());
        descuento.setPorcentaje(request.getPorcentaje());
        descuento.setFechaInicio(request.getFechaInicio());
        descuento.setFechaFin(request.getFechaFin());
        descuento.setEstado(EstadoRegistro.ACTIVO.getValor());

        return mapToResponse(descuentoRepository.save(descuento));
    }

    public List<DescuentoResponseDTO> listarDescuentos() {
        return descuentoRepository.listarActivos().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DescuentoResponseDTO obtenerPorId(Integer id) {
        Descuento descuento = descuentoRepository.findByIdActivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado con ID: " + id));
        return mapToResponse(descuento);
    }

    public DescuentoResponseDTO actualizarDescuento(Integer id, DescuentoUpdateDTO request) {
        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado"));

        if (descuento.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede actualizar un descuento eliminado");
        }

        validarFechas(request.getFechaInicio(), request.getFechaFin());

        descuento.setNombre(request.getNombre());
        descuento.setTipoCondicion(request.getTipoCondicion());
        descuento.setValorCondicion(request.getValorCondicion());
        descuento.setPorcentaje(request.getPorcentaje());
        descuento.setFechaInicio(request.getFechaInicio());
        descuento.setFechaFin(request.getFechaFin());

        return mapToResponse(descuentoRepository.save(descuento));
    }

    public DescuentoResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }
        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado"));

        if (descuento.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede cambiar el estado de un descuento eliminado");
        }

        descuento.setEstado(nuevoEstado);
        
        // Lógica de fechas
        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            descuento.setFechaDesactivacion(java.time.LocalDateTime.now());
        } else {
            descuento.setFechaDesactivacion(null);
        }

        return mapToResponse(descuentoRepository.save(descuento));
    }

    public void eliminarLogicamente(Integer id) {
        Descuento descuento = descuentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado"));

        if (descuento.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("El descuento ya está eliminado");
        }

        descuento.setEstado(EstadoRegistro.ELIMINADO.getValor());
        // Lógica de fecha
        descuento.setFechaEliminacion(java.time.LocalDateTime.now());
        
        descuentoRepository.save(descuento);
    }

    private void validarFechas(java.time.LocalDate inicio, java.time.LocalDate fin) {
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private DescuentoResponseDTO mapToResponse(Descuento d) {
        return new DescuentoResponseDTO(
                d.getId(),
                d.getProducto().getNombre(),
                d.getNombre(),
                d.getTipoCondicion(),
                d.getValorCondicion(),
                d.getPorcentaje(),
                d.getFechaInicio(),
                d.getFechaFin(),
                d.getEstado()
        );
    }
}


