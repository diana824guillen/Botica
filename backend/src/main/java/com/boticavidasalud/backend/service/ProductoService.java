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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PresentacionRepository presentacionRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            PresentacionRepository presentacionRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.presentacionRepository = presentacionRepository;
    }

    // ============================================================
    // CREAR PRODUCTO
    // ============================================================

    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {

        String nombre = normalizarNombre(request.getNombre());

        if (nombre.isBlank()) {
            throw new BusinessException(
                    "El nombre del producto es obligatorio"
            );
        }

        Categoria categoria = categoriaRepository
                .findById(request.getCategoriaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Categoría no encontrada con ID: "
                                        + request.getCategoriaId()
                        )
                );

        if (!categoria.getEstado().equals(
                EstadoRegistro.ACTIVO.getValor())) {
            throw new BusinessException(
                    "Solo puede asignar categorías activas a un producto nuevo"
            );
        }

        productoRepository.findByNombre(nombre).ifPresent(p -> {
            throw new BusinessException(
                    "Ya existe un producto con ese nombre"
            );
        });

        List<PresentacionDTO> solicitudes =
                request.getPresentaciones();

        if (solicitudes == null || solicitudes.isEmpty()) {
            throw new BusinessException(
                    "Agregue al menos una presentación al producto"
            );
        }

        // Evitar presentaciones duplicadas dentro del mismo producto nuevo.
        Set<String> combinaciones = new HashSet<>();

        for (PresentacionDTO dto : solicitudes) {

            validarDatosPresentacion(dto);

            String clave = crearClavePresentacion(
                    dto.getTipo().name(),
                    dto.getFactorConversion()
            );

            if (!combinaciones.add(clave)) {
                throw new BusinessException(
                        "No puede repetir el mismo tipo y factor de conversión"
                );
            }
        }

        Producto producto = new Producto();

        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setDescripcion(
                normalizarDescripcion(request.getDescripcion())
        );
        producto.setRequiereReceta(
                request.getRequiereReceta() != null
                        ? request.getRequiereReceta()
                        : false
        );
        producto.setStockMinimo(
                request.getStockMinimo() != null
                        ? request.getStockMinimo()
                        : BigDecimal.ZERO
        );
        producto.setEstado(EstadoRegistro.ACTIVO.getValor());

        List<Presentacion> presentaciones = new ArrayList<>();

        for (PresentacionDTO dto : solicitudes) {

            Presentacion presentacion = new Presentacion();

            presentacion.setProducto(producto);
            presentacion.setTipo(dto.getTipo());
            presentacion.setFactorConversion(dto.getFactorConversion());
            presentacion.setPrecioVenta(dto.getPrecioVenta());
            presentacion.setEstado(EstadoRegistro.ACTIVO.getValor());

            presentaciones.add(presentacion);
        }

        producto.setPresentaciones(presentaciones);

        // La relación Producto-Presentación tiene cascade ALL.
        Producto guardado = productoRepository.save(producto);

        return mapToResponse(guardado);
    }

    // ============================================================
    // LISTAR PRODUCTOS
    // ============================================================

    public List<ProductoResponseDTO> listarProductos() {

        return productoRepository.listarActivos()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosPorCategoria(
            Integer categoriaId) {

        return productoRepository.listarPorCategoria(categoriaId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    public ProductoResponseDTO obtenerProductoPorId(Integer id) {

        Producto producto = productoRepository.findByIdActivo(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + id
                        )
                );

        return mapToResponse(producto);
    }

    // ============================================================
    // LISTAR PRESENTACIONES ACTIVAS E INACTIVAS
    // ============================================================

    public List<PresentacionInfoDTO> listarPresentaciones(
            Integer idProducto) {

        productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        return presentacionRepository.findByProductoId(idProducto)
                .stream()
                // Excluir eliminadas lógicamente, pero mostrar inactivas.
                .filter(p -> !EstadoRegistro.ELIMINADO.getValor()
                        .equals(p.getEstado()))
                .map(this::mapPresentacionToInfo)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ACTUALIZAR PRODUCTO
    // ============================================================

    public ProductoResponseDTO actualizarProducto(
            Integer id,
            ProductoUpdateDTO request) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + id
                        )
                );

        if (producto.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "No se puede actualizar un producto eliminado"
            );
        }

        String nombre = normalizarNombre(request.getNombre());

        if (nombre.isBlank()) {
            throw new BusinessException(
                    "El nombre del producto es obligatorio"
            );
        }

        productoRepository
                .findByNombreAndIdNot(nombre, id)
                .ifPresent(p -> {
                    throw new BusinessException(
                            "Ya existe otro producto con ese nombre"
                    );
                });

        Categoria categoria = categoriaRepository
                .findById(request.getCategoriaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Categoría no encontrada con ID: "
                                        + request.getCategoriaId()
                        )
                );

        // Si la categoría está inactiva o eliminada, solo se permite conservarla si ya era la categoría del producto.
        boolean mismaCategoria = producto.getCategoria() != null
                && producto.getCategoria().getId()
                        .equals(categoria.getId());

        if (!mismaCategoria && !categoria.getEstado().equals(
                EstadoRegistro.ACTIVO.getValor())) {
            throw new BusinessException(
                    "Solo puede asignar categorías activas a un producto"
            );
        }

        producto.setCategoria(categoria);
        producto.setNombre(nombre);
        producto.setDescripcion(
                normalizarDescripcion(request.getDescripcion())
        );
        producto.setRequiereReceta(
                request.getRequiereReceta() != null
                        ? request.getRequiereReceta()
                        : false
        );
        producto.setStockMinimo(
                request.getStockMinimo() != null
                        ? request.getStockMinimo()
                        : BigDecimal.ZERO
        );

        // Si se envían presentaciones con ID, actualizar esos registros.
        // Las nuevas presentaciones se gestionan mediante su endpoint POST.
        if (request.getPresentaciones() != null) {

            for (PresentacionDTO dto : request.getPresentaciones()) {

                if (dto.getId() == null) {
                    continue;
                }

                validarDatosPresentacion(dto);

                Presentacion existente = obtenerPresentacionDelProducto(
                        id,
                        dto.getId()
                );

                if (existente.getEstado().equals(
                        EstadoRegistro.ELIMINADO.getValor())) {
                    throw new BusinessException(
                            "No se puede actualizar una presentación eliminada"
                    );
                }

                validarPresentacionNoDuplicada(
                        id,
                        dto,
                        existente.getId()
                );

                existente.setTipo(dto.getTipo());
                existente.setFactorConversion(dto.getFactorConversion());
                existente.setPrecioVenta(dto.getPrecioVenta());
            }
        }

        Producto actualizado = productoRepository.save(producto);

        return mapToResponse(actualizado);
    }

    // ============================================================
    // AGREGAR PRESENTACIONES A UN PRODUCTO EXISTENTE
    // ============================================================

    public ProductoResponseDTO agregarPresentaciones(
            Integer idProducto,
            List<PresentacionDTO> presentaciones) {

        Producto producto = productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        if (presentaciones == null || presentaciones.isEmpty()) {
            throw new BusinessException(
                    "Debe enviar al menos una presentación"
            );
        }

        Set<String> combinacionesNuevas = new HashSet<>();

        for (PresentacionDTO dto : presentaciones) {

            validarDatosPresentacion(dto);

            String clave = crearClavePresentacion(dto.getTipo().name(), dto.getFactorConversion());

            if (!combinacionesNuevas.add(clave)) {
                throw new BusinessException("No puede repetir el mismo tipo y factor de conversión");
            }

            validarPresentacionNoDuplicada(
                    idProducto,
                    dto,
                    null
            );

            Presentacion nueva = new Presentacion();

            nueva.setProducto(producto);
            nueva.setTipo(dto.getTipo());
            nueva.setFactorConversion(dto.getFactorConversion());
            nueva.setPrecioVenta(dto.getPrecioVenta());
            nueva.setEstado(EstadoRegistro.ACTIVO.getValor());

            producto.getPresentaciones().add(nueva);
        }

        Producto actualizado = productoRepository.save(producto);

        return mapToResponse(actualizado);
    }

    // ============================================================
    // ACTUALIZAR UNA PRESENTACIÓN
    // ============================================================

    public PresentacionInfoDTO actualizarPresentacion(
            Integer idProducto,
            Integer idPresentacion,
            PresentacionDTO request) {

        productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        Presentacion presentacion = obtenerPresentacionDelProducto(
                idProducto,
                idPresentacion
        );

        if (presentacion.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "No se puede actualizar una presentación eliminada"
            );
        }

        validarDatosPresentacion(request);

        validarPresentacionNoDuplicada(
                idProducto,
                request,
                idPresentacion
        );

        presentacion.setTipo(request.getTipo());
        presentacion.setFactorConversion(request.getFactorConversion());
        presentacion.setPrecioVenta(request.getPrecioVenta());

        Presentacion actualizada =
                presentacionRepository.save(presentacion);

        return mapPresentacionToInfo(actualizada);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR PRESENTACIÓN
    // ============================================================

    public ProductoResponseDTO cambiarEstadoPresentacion(
            Integer idProducto,
            Integer idPresentacion,
            Short nuevoEstado) {

        if (nuevoEstado == null ||
                (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)"
            );
        }

        Producto producto = productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        Presentacion presentacion = obtenerPresentacionDelProducto(
                idProducto,
                idPresentacion
        );

        if (presentacion.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "No se puede cambiar el estado de una presentación eliminada"
            );
        }

        if (nuevoEstado.equals(EstadoRegistro.ACTIVO.getValor())) {

            PresentacionDTO dto = new PresentacionDTO();

            dto.setTipo(presentacion.getTipo());
            dto.setFactorConversion(presentacion.getFactorConversion());
            dto.setPrecioVenta(presentacion.getPrecioVenta());

            validarPresentacionNoDuplicada(
                    idProducto,
                    dto,
                    idPresentacion
            );
        }

        // No permitir que un producto activo se quede sin presentaciones
        // activas para su venta.
        if (
                nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())
                && presentacion.getEstado().equals(
                        EstadoRegistro.ACTIVO.getValor())
                && producto.getEstado().equals(
                        EstadoRegistro.ACTIVO.getValor())
                && contarPresentacionesActivas(idProducto) <= 1
        ) {
            throw new BusinessException(
                    "No se puede desactivar la última presentación activa. Agregue otra presentación primero."
            );
        }

        presentacion.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            presentacion.setFechaDesactivacion(LocalDateTime.now());
        } else {
            presentacion.setFechaDesactivacion(null);
        }

        presentacionRepository.save(presentacion);

        return mapToResponse(producto);
    }

    // ============================================================
    // ELIMINAR PRESENTACIÓN LÓGICAMENTE
    // ============================================================

    public void eliminarPresentacion(
            Integer idProducto,
            Integer idPresentacion) {

        Producto producto = productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        Presentacion presentacion = obtenerPresentacionDelProducto(
                idProducto,
                idPresentacion
        );

        if (presentacion.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "La presentación ya está eliminada"
            );
        }

        if (
                presentacion.getEstado().equals(
                        EstadoRegistro.ACTIVO.getValor())
                && producto.getEstado().equals(
                        EstadoRegistro.ACTIVO.getValor())
                && contarPresentacionesActivas(idProducto) <= 1
        ) {
            throw new BusinessException(
                    "No se puede eliminar la última presentación activa de un producto. Agregue otra presentación primero."
            );
        }

        presentacion.setEstado(EstadoRegistro.ELIMINADO.getValor());
        presentacion.setFechaEliminacion(LocalDateTime.now());

        presentacionRepository.save(presentacion);
    }

    // ============================================================
    // CAMBIAR ESTADO DEL PRODUCTO
    // ============================================================

    public ProductoResponseDTO cambiarEstado(
            Integer id,
            Short nuevoEstado) {

        if (nuevoEstado == null ||
                (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)"
            );
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + id
                        )
                );

        if (producto.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "No se puede cambiar el estado de un producto eliminado"
            );
        }

        if (
                nuevoEstado.equals(EstadoRegistro.ACTIVO.getValor())
                && contarPresentacionesActivas(id) == 0
        ) {
            throw new BusinessException(
                    "No se puede activar un producto sin presentaciones activas"
            );
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
    // ELIMINACIÓN LÓGICA DEL PRODUCTO
    // ============================================================

    public void eliminarLogicamente(Integer id) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + id
                        )
                );

        if (producto.getEstado().equals(
                EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException(
                    "El producto ya está eliminado"
            );
        }

        producto.setEstado(EstadoRegistro.ELIMINADO.getValor());
        producto.setFechaEliminacion(LocalDateTime.now());

        productoRepository.save(producto);
    }

    // ============================================================
    // CONSULTAS DE STOCK Y VENCIMIENTOS
    // ============================================================

    public List<ProductoResponseDTO> listarProductosConStockBajo() {

        return productoRepository.listarConStockBajo()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesPorVencer(
            int dias) {

        if (dias < 0) {
            throw new BusinessException(
                    "Los días de anticipación no pueden ser negativos"
            );
        }

        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);

        return productoRepository
                .listarConLotesPorVencer(hoy, limite)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarProductosConLotesVencidos() {

        return productoRepository
                .listarConLotesVencidos(LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // VALIDACIONES AUXILIARES DE PRESENTACIONES
    // ============================================================

    private void validarDatosPresentacion(PresentacionDTO dto) {

        if (dto == null || dto.getTipo() == null) {
            throw new BusinessException(
                    "El tipo de presentación es obligatorio"
            );
        }

        if (dto.getFactorConversion() == null ||
                dto.getFactorConversion() <= 0) {
            throw new BusinessException(
                    "El factor de conversión debe ser mayor a 0"
            );
        }

        if (dto.getPrecioVenta() == null ||
                dto.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "El precio de venta debe ser mayor a 0"
            );
        }
    }

    private void validarPresentacionNoDuplicada(
            Integer idProducto,
            PresentacionDTO dto,
            Integer idPresentacionExcluir) {

        boolean duplicada = presentacionRepository
                .findByProductoId(idProducto)
                .stream()
                .anyMatch(p ->
                        !p.getId().equals(idPresentacionExcluir)
                        && p.getTipo().equals(dto.getTipo())
                        && p.getFactorConversion().equals(dto.getFactorConversion())
                );

        if (duplicada) {
            throw new BusinessException(
                    "Ya existe una presentación con ese tipo y factor de conversión para este producto"
            );
        }
    }

    private Presentacion obtenerPresentacionDelProducto(
            Integer idProducto,
            Integer idPresentacion) {

        return presentacionRepository.findByProductoId(idProducto)
                .stream()
                .filter(p -> p.getId().equals(idPresentacion))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Presentación no encontrada para este producto"
                        )
                );
    }

    private long contarPresentacionesActivas(Integer idProducto) {

        return presentacionRepository.findByProductoId(idProducto)
                .stream()
                .filter(p ->
                        EstadoRegistro.ACTIVO.getValor().equals(p.getEstado())
                )
                .count();
    }

    private String crearClavePresentacion(
            String tipo,
            Integer factorConversion) {

        return tipo + "|" + factorConversion;
    }

    // ============================================================
    // NORMALIZACIÓN
    // ============================================================

    private String normalizarNombre(String nombre) {

        if (nombre == null) {
            return "";
        }

        return nombre.trim().replaceAll("\\s+", " ");
    }

    private String normalizarDescripcion(String descripcion) {

        if (descripcion == null) {
            return null;
        }

        String resultado = descripcion.trim();

        return resultado.isEmpty() ? null : resultado;
    }

    // ============================================================
    // MAPEO DE RESPUESTAS
    // ============================================================

    private ProductoResponseDTO mapToResponse(Producto producto) {

        ProductoResponseDTO dto = new ProductoResponseDTO();

        dto.setId(producto.getId());
        dto.setCategoriaId(producto.getCategoria().getId());
        dto.setCategoriaNombre(producto.getCategoria().getNombre());

        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());

        dto.setRequiereReceta(producto.getRequiereReceta());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setEstado(producto.getEstado());
        dto.setFechaCreacion(producto.getFechaCreacion());

        BigDecimal stockActual = BigDecimal.ZERO;

        if (producto.getLotes() != null) {
        stockActual = producto.getLotes()
                .stream()
                .filter(l ->
                        EstadoRegistro.ACTIVO.getValor().equals(l.getEstado())
                )
                .map(l -> l.getCantidad() == null
                        ? BigDecimal.ZERO
                        : l.getCantidad())
                .reduce(BigDecimal.ZERO,
                        (acumulado, valor) -> acumulado.add(valor));
        }

        dto.setStockActual(stockActual);

        List<PresentacionInfoDTO> presentaciones =
                producto.getPresentaciones() != null
                        ? producto.getPresentaciones()
                                .stream()
                                .filter(p ->
                                        EstadoRegistro.ACTIVO.getValor()
                                                .equals(p.getEstado())
                                )
                                .map(this::mapPresentacionToInfo)
                                .collect(Collectors.toList())
                        : new ArrayList<>();

        dto.setPresentaciones(presentaciones);

        return dto;
    }

    private PresentacionInfoDTO mapPresentacionToInfo(
            Presentacion presentacion) {

        PresentacionInfoDTO dto = new PresentacionInfoDTO();

        dto.setId(presentacion.getId());
        dto.setTipo(presentacion.getTipo());
        dto.setFactorConversion(presentacion.getFactorConversion());
        dto.setPrecioVenta(presentacion.getPrecioVenta());
        dto.setEstado(presentacion.getEstado());

        return dto;
    }
}


