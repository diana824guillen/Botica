package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.*;
import com.boticavidasalud.backend.entity.Categoria;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.CategoriaRepository;
import com.boticavidasalud.backend.repository.PresentacionRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           PresentacionRepository presentacionRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // ============================================================
    // CREAR PRODUCTO
    // ============================================================
    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        productoRepository.findByNombre(request.getNombre()).ifPresent(p -> {
            throw new BusinessException("Ya existe un producto con ese nombre");
        });

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setRequiereReceta(request.getRequiereReceta() != null ? request.getRequiereReceta() : false);
        producto.setStockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : BigDecimal.ZERO);
        producto.setEstado(EstadoRegistro.ACTIVO.getValor());

        // Construir presentaciones antes de guardar
        List<Presentacion> presentaciones = new ArrayList<>();
        if (request.getPresentaciones() != null) {
            for (PresentacionDTO dto : request.getPresentaciones()) {
                Presentacion p = new Presentacion();
                p.setProducto(producto);
                p.setTipo(dto.getTipo());
                p.setFactorConversion(dto.getFactorConversion() != null ? dto.getFactorConversion() : 1);
                p.setPrecioVenta(dto.getPrecioVenta());
                p.setEstado(EstadoRegistro.ACTIVO.getValor());
                presentaciones.add(p);
            }
        }
        producto.setPresentaciones(presentaciones);

        Producto savedProducto = productoRepository.save(producto);
        return mapToResponse(savedProducto);
    }

    // ============================================================
    // LISTAR PRODUCTOS
    // ============================================================
    public List<ProductoResponseDTO> listarProductos() {
        return productoRepository.listarActivos().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosPorCategoria(Integer categoriaId) {
        return productoRepository.listarPorCategoria(categoriaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================
    public ProductoResponseDTO obtenerProductoPorId(Integer id) {
        Producto producto = productoRepository.findByIdActivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return mapToResponse(producto);
    }

    // ============================================================
    // ACTUALIZAR PRODUCTO
    // ============================================================
    public ProductoResponseDTO actualizarProducto(Integer id, ProductoUpdateDTO request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (producto.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede actualizar un producto eliminado");
        }

        productoRepository.findByNombreAndIdNot(request.getNombre(), id).ifPresent(p -> {
            throw new BusinessException("Ya existe otro producto con ese nombre");
        });

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        producto.setCategoria(categoria);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setRequiereReceta(request.getRequiereReceta() != null ? request.getRequiereReceta() : false);
        producto.setStockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : BigDecimal.ZERO);

        // Solo actualizar presentaciones que vengan con id.
        if (request.getPresentaciones() != null && !request.getPresentaciones().isEmpty()) {
            for (PresentacionDTO dto : request.getPresentaciones()) {
                if (dto.getId() == null) {
                    continue; // Ignorar, el frontend debe usar el endpoint POST para crear
                }
                Presentacion existente = producto.getPresentaciones().stream()
                        .filter(p -> p.getId().equals(dto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Presentación no encontrada con ID: " + dto.getId()));

                existente.setTipo(dto.getTipo());
                existente.setFactorConversion(dto.getFactorConversion());
                existente.setPrecioVenta(dto.getPrecioVenta());

                if (existente.getEstado() != 1) {
                    existente.setEstado(EstadoRegistro.ACTIVO.getValor());
                    existente.setFechaDesactivacion(null);
                }
            }
        }

        Producto actualizado = productoRepository.save(producto);
        return mapToResponse(actualizado);
    }

    // ============================================================
    // AGREGAR PRESENTACIONES A UN PRODUCTO EXISTENTE
    // ============================================================
    public ProductoResponseDTO agregarPresentaciones(Integer idProducto, List<PresentacionDTO> presentaciones) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + idProducto));

        if (producto.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede agregar presentaciones a un producto eliminado");
        }

        if (presentaciones == null || presentaciones.isEmpty()) {
            throw new BusinessException("Debe enviar al menos una presentación");
        }

        List<Presentacion> actuales = producto.getPresentaciones();

        for (PresentacionDTO dto : presentaciones) {
            // Verificar que no exista ya (tipo + factor) activa
            boolean existe = actuales.stream()
                    .anyMatch(p -> p.getTipo().equals(dto.getTipo())
                                && p.getFactorConversion().equals(dto.getFactorConversion())
                                && p.getEstado() == 1);
            if (existe) {
                throw new BusinessException(
                    "Ya existe una presentación " + dto.getTipo() + " con factor " + dto.getFactorConversion()
                );
            }

            Presentacion nueva = new Presentacion();
            nueva.setProducto(producto);
            nueva.setTipo(dto.getTipo());
            nueva.setFactorConversion(dto.getFactorConversion() != null ? dto.getFactorConversion() : 1);
            nueva.setPrecioVenta(dto.getPrecioVenta());
            nueva.setEstado(EstadoRegistro.ACTIVO.getValor());
            actuales.add(nueva);
        }

        productoRepository.save(producto);
        return mapToResponse(producto);
    }

    // ============================================================
    // CAMBIAR ESTADO DE PRESENTACIÓN
    // ============================================================
    public ProductoResponseDTO cambiarEstadoPresentacion(Integer idProducto, Integer idPresentacion, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + idProducto));

        Presentacion presentacion = producto.getPresentaciones().stream()
                .filter(p -> p.getId().equals(idPresentacion))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Presentación no encontrada con ID: " + idPresentacion));

        presentacion.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            presentacion.setFechaDesactivacion(LocalDateTime.now());
        } else {
            presentacion.setFechaDesactivacion(null);
        }

        productoRepository.save(producto);
        return mapToResponse(producto);
    }

    // ============================================================
    // CAMBIAR ESTADO DEL PRODUCTO
    // ============================================================
    public ProductoResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (producto.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede cambiar el estado de un producto eliminado");
        }

        producto.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            producto.setFechaDesactivacion(LocalDateTime.now());
        } else {
            producto.setFechaDesactivacion(null);
        }

        Producto actualizado = productoRepository.save(producto);
        return mapToResponse(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA
    // ============================================================
    public void eliminarLogicamente(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (producto.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("El producto ya está eliminado");
        }

        producto.setEstado(EstadoRegistro.ELIMINADO.getValor());
        producto.setFechaEliminacion(LocalDateTime.now());
        productoRepository.save(producto);
    }

    // ============================================================
    // CONSULTAS ESPECÍFICAS
    // ============================================================
    public List<ProductoResponseDTO> listarProductosConStockBajo() {
        return productoRepository.listarConStockBajo().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return productoRepository.listarConLotesPorVencer(hoy, limite).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesVencidos() {
        return productoRepository.listarConLotesVencidos(LocalDate.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // MAPEO
    // ============================================================
    private ProductoResponseDTO mapToResponse(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setCategoriaNombre(producto.getCategoria().getNombre());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setRequiereReceta(producto.getRequiereReceta());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setEstado(producto.getEstado());
        dto.setFechaCreacion(producto.getFechaCreacion());

        BigDecimal stockActual = BigDecimal.ZERO;
        if (producto.getLotes() != null) {
            stockActual = producto.getLotes().stream()
                    .filter(l -> l.getEstado() == 1)
                    .map(l -> l.getCantidad())
                    .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        }
        dto.setStockActual(stockActual);

        List<PresentacionInfoDTO> presDTOs = producto.getPresentaciones() != null
                ? producto.getPresentaciones().stream()
                    .filter(p -> p.getEstado() == 1)
                    .map(this::mapPresentacionToInfo)
                    .collect(Collectors.toList())
                : new ArrayList<>();
        dto.setPresentaciones(presDTOs);

        return dto;
    }

    private PresentacionInfoDTO mapPresentacionToInfo(Presentacion p) {
        PresentacionInfoDTO dto = new PresentacionInfoDTO();
        dto.setId(p.getId());
        dto.setTipo(p.getTipo());
        dto.setFactorConversion(p.getFactorConversion());
        dto.setPrecioVenta(p.getPrecioVenta());
        return dto;
    }
}


