package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.ProductoRequestDTO;
import com.boticavidasalud.backend.dto.ProductoResponseDTO;
import com.boticavidasalud.backend.dto.PresentacionDTO;
import com.boticavidasalud.backend.dto.PresentacionInfoDTO;
import com.boticavidasalud.backend.entity.Categoria;
import com.boticavidasalud.backend.entity.Lote;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.CategoriaRepository;
import com.boticavidasalud.backend.repository.LoteRepository;
import com.boticavidasalud.backend.repository.PresentacionRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PresentacionRepository presentacionRepository;
    private final LoteRepository loteRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           PresentacionRepository presentacionRepository,
                           LoteRepository loteRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.presentacionRepository = presentacionRepository;
        this.loteRepository = loteRepository;
    }

    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        Producto producto = new Producto();
        producto.setCategoria(categoria);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setRequiereReceta(request.getRequiereReceta() != null ? request.getRequiereReceta() : false);
        producto.setStockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : BigDecimal.ZERO);
        producto.setEstado((short) 1);

        Producto savedProducto = productoRepository.save(producto);

        if (request.getPresentaciones() != null && !request.getPresentaciones().isEmpty()) {
            for (PresentacionDTO presDTO : request.getPresentaciones()) {
                Presentacion presentacion = new Presentacion();
                presentacion.setProducto(savedProducto);
                presentacion.setTipo(presDTO.getTipo());
                presentacion.setFactorConversion(presDTO.getFactorConversion() != null ? presDTO.getFactorConversion() : 1);
                presentacion.setPrecioVenta(presDTO.getPrecioVenta());
                presentacion.setEstado((short) 1);
                
                Presentacion savedPresentacion = presentacionRepository.save(presentacion);
                savedProducto.getPresentaciones().add(savedPresentacion);
            }
        }

        return mapToResponse(savedProducto);
    }

    public ProductoResponseDTO obtenerProductoPorId(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return mapToResponse(producto);
    }

    public List<ProductoResponseDTO> listarProductos() {
        return productoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosPorCategoria(Integer categoriaId) {
        return productoRepository.findByCategoriaId(categoriaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConStockBajo() {
        return productoRepository.findProductosConStockBajo().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        List<Lote> lotesPorVencer = loteRepository.findLotesPorVencer(hoy, limite);

        Set<Producto> productos = lotesPorVencer.stream()
                .map(lote -> lote.getProducto())
                .collect(Collectors.toSet());

        return productos.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesVencidos() {
        List<Lote> lotesVencidos = loteRepository.findLotesVencidos(LocalDate.now());
        Set<Producto> productos = lotesVencidos.stream()
                .map(lote -> lote.getProducto())
                .collect(Collectors.toSet());
        return productos.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // -------------------- Método de mapeo --------------------
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

        BigDecimal stockActual = producto.getLotes().stream()
                .filter(lote -> lote.getEstado() == 1)
                .map(lote -> lote.getCantidad())
                .reduce(BigDecimal.ZERO, (acum, cantidad) -> acum.add(cantidad));
        dto.setStockActual(stockActual);

        Set<PresentacionInfoDTO> presentacionesDTO = producto.getPresentaciones().stream()
                .map(this::mapPresentacionToInfo)
                .collect(Collectors.toSet());
        dto.setPresentaciones(presentacionesDTO);

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


