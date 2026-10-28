package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.DetalleIngresoDTO;
import com.boticavidasalud.backend.dto.LoteAjusteDTO;
import com.boticavidasalud.backend.dto.LoteRequestDTO;
import com.boticavidasalud.backend.dto.LoteResponseDTO;

import com.boticavidasalud.backend.entity.Lote;
import com.boticavidasalud.backend.entity.MovimientoInventario;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.entity.Usuario;

import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.enums.TipoMovimiento;

import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;

import com.boticavidasalud.backend.repository.LoteRepository;
import com.boticavidasalud.backend.repository.MovimientoInventarioRepository;
import com.boticavidasalud.backend.repository.PresentacionRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import com.boticavidasalud.backend.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class LoteService {

    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PresentacionRepository presentacionRepository;

    public LoteService(
            LoteRepository loteRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            MovimientoInventarioRepository movimientoRepository,
            PresentacionRepository presentacionRepository) {

        this.loteRepository = loteRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.presentacionRepository = presentacionRepository;
    }

    // ============================================================
    // CREAR LOTE
    // ============================================================

    public LoteResponseDTO crearLote(
            Integer idProducto,
            LoteRequestDTO request) {

        if (request == null) {
            throw new BusinessException(
                    "Los datos del lote son obligatorios"
            );
        }

        // Normalizar el número de lote.
        String numeroLote = normalizarNumeroLote(
                request.getNumeroLote()
        );

        if (numeroLote.isBlank()) {
            throw new BusinessException(
                    "El número de lote es obligatorio"
            );
        }

        if (numeroLote.length() > 50) {
            throw new BusinessException(
                    "El número de lote no puede superar los 50 caracteres"
            );
        }

        if (request.getFechaVencimiento() == null
                || !request.getFechaVencimiento().isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "La fecha de vencimiento debe ser futura"
            );
        }

        request.setNumeroLote(numeroLote);

        // Buscar el producto.
        Producto producto = productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con ID: " + idProducto
                        )
                );

        // Solo se permiten ingresos a productos activos.
        if (!EstadoRegistro.ACTIVO.getValor().equals(
                producto.getEstado())) {
            throw new BusinessException(
                    "Solo puede registrar lotes para productos activos"
            );
        }

        // Validar que no exista otro lote con el mismo número
        // para este producto.
        loteRepository.findByProductoAndNumeroLote(
                idProducto,
                numeroLote
        ).ifPresent(l -> {
            throw new BusinessException(
                    "Ya existe un lote con ese número para este producto"
            );
        });

        List<DetalleIngresoDTO> detalles = request.getDetallesIngreso();

        if (detalles == null || detalles.isEmpty()) {
            throw new BusinessException(
                    "Debe registrar al menos una presentación en el ingreso"
            );
        }

        // Validar las presentaciones y calcular el stock total
        // convertido a la unidad base del producto.
        BigDecimal cantidadTotalBase = BigDecimal.ZERO;

        Set<Integer> presentacionesProcesadas = new HashSet<>();
        List<Presentacion> presentacionesIngreso = new ArrayList<>();

        for (DetalleIngresoDTO detalle : detalles) {

            if (detalle == null
                    || detalle.getIdPresentacion() == null
                    || detalle.getCantidadPresentacion() == null) {
                throw new BusinessException(
                        "Los datos de cada presentación son obligatorios"
                );
            }

            BigDecimal cantidadPresentacion =
                    detalle.getCantidadPresentacion();

            if (cantidadPresentacion.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "La cantidad de cada presentación debe ser mayor a cero"
                );
            }

            validarCantidadEntera(
                    cantidadPresentacion,
                    "La cantidad recibida debe ser un número entero"
            );

            if (!presentacionesProcesadas.add(
                    detalle.getIdPresentacion())) {
                throw new BusinessException(
                        "No puede repetir una presentación dentro del mismo ingreso"
                );
            }

            Presentacion presentacion = presentacionRepository
                    .findById(detalle.getIdPresentacion())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Presentación no encontrada con ID: "
                                            + detalle.getIdPresentacion()
                            )
                    );

            // Verificar que la presentación pertenezca al producto.
            if (presentacion.getProducto() == null
                    || !presentacion.getProducto().getId().equals(idProducto)) {
                throw new BusinessException(
                        "La presentación no pertenece al producto seleccionado"
                );
            }

            // No se permiten presentaciones inactivas o eliminadas.
            if (!EstadoRegistro.ACTIVO.getValor().equals(
                    presentacion.getEstado())) {
                throw new BusinessException(
                        "Solo puede registrar ingresos con presentaciones activas"
                );
            }

            Integer factorConversion =
                    presentacion.getFactorConversion();

            if (factorConversion == null || factorConversion <= 0) {
                throw new BusinessException(
                        "El factor de conversión de la presentación no es válido"
                );
            }

            BigDecimal factor = BigDecimal.valueOf(factorConversion);

            BigDecimal cantidadBase =
                    cantidadPresentacion.multiply(factor);

            cantidadTotalBase =
                    cantidadTotalBase.add(cantidadBase);

            presentacionesIngreso.add(presentacion);
        }

        // Crear el lote con la suma total expresada en unidad base.
        Lote lote = new Lote();

        lote.setProducto(producto);
        lote.setNumeroLote(numeroLote);
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setCantidad(cantidadTotalBase);
        lote.setEstado(EstadoRegistro.ACTIVO.getValor());

        Lote guardado = loteRepository.save(lote);

        // Registrar el movimiento de cada presentación recibida.
        // El stock anterior y nuevo se acumulan correctamente.
        BigDecimal stockAcumulado = BigDecimal.ZERO;

        for (DetalleIngresoDTO detalle : request.getDetallesIngreso()) {

        Presentacion presentacion = presentacionRepository
                .findById(detalle.getIdPresentacion())
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Presentación no encontrada ID: "
                                + detalle.getIdPresentacion()
                                )
                );

        BigDecimal factor = BigDecimal.valueOf(presentacion.getFactorConversion());

        BigDecimal cantidadBase = detalle.getCantidadPresentacion().multiply(factor);

        BigDecimal stockAnterior = stockAcumulado;
        stockAcumulado = stockAcumulado.add(cantidadBase);

        registrarMovimiento(
                guardado,
                presentacion.getId(),
                detalle.getCantidadPresentacion(),
                TipoMovimiento.INGRESO,
                stockAnterior,
                cantidadBase,
                stockAcumulado,
                "Ingreso inicial de lote en " + presentacion.getTipo()
        );
        }

        return mapToResponse(guardado);
    }

    // ============================================================
    // AJUSTAR EXISTENCIAS DE UN LOTE
    // ============================================================

    public LoteResponseDTO ajustarLote(
            Integer id,
            LoteAjusteDTO request) {

        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lote no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                lote.getEstado())) {
            throw new BusinessException(
                    "No se puede ajustar un lote eliminado"
            );
        }

        if (request == null
                || request.getIdPresentacion() == null
                || request.getCantidadAjuste() == null
                || request.getMotivo() == null) {
            throw new BusinessException(
                    "Los datos del ajuste son obligatorios"
            );
        }

        BigDecimal cantidadAjuste = request.getCantidadAjuste();

        if (cantidadAjuste.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(
                    "La cantidad del ajuste no puede ser cero"
            );
        }

        validarCantidadEntera(
                cantidadAjuste,
                "La cantidad del ajuste debe ser un número entero"
        );

        // Una merma siempre debe descontar existencias.
        if (request.getMotivo().getTipoMovimiento() == TipoMovimiento.MERMA
                && cantidadAjuste.compareTo(BigDecimal.ZERO) >= 0) {
            throw new BusinessException(
                    "La cantidad de una merma debe ser negativa"
            );
        }

        Presentacion presentacion = presentacionRepository
                .findById(request.getIdPresentacion())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Presentación no encontrada"
                        )
                );

        if (presentacion.getProducto() == null
                || !presentacion.getProducto().getId()
                        .equals(lote.getProducto().getId())) {
            throw new BusinessException(
                    "La presentación no pertenece al producto de este lote"
            );
        }

        if (!EstadoRegistro.ACTIVO.getValor().equals(
                presentacion.getEstado())) {
            throw new BusinessException(
                    "Solo puede realizar ajustes con presentaciones activas"
            );
        }

        Integer factorConversion = presentacion.getFactorConversion();

        if (factorConversion == null || factorConversion <= 0) {
            throw new BusinessException(
                    "El factor de conversión de la presentación no es válido"
            );
        }

        BigDecimal factor = BigDecimal.valueOf(factorConversion);

        // La cantidad está expresada en la presentación seleccionada.
        // Se convierte a la unidad base, conservando el signo.
        BigDecimal cantidadBaseAjuste =
                cantidadAjuste.multiply(factor);

        BigDecimal stockAnterior = lote.getCantidad();

        BigDecimal stockNuevo =
                stockAnterior.add(cantidadBaseAjuste);

        if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "El ajuste dejaría el stock en negativo"
            );
        }

        lote.setCantidad(stockNuevo);

        Lote actualizado = loteRepository.save(lote);

        TipoMovimiento tipoMovimiento =
                request.getMotivo().getTipoMovimiento();

        String motivoFinal =
                request.getMotivo().getDescripcion();

        if (request.getObservacion() != null
                && !request.getObservacion().isBlank()) {
            motivoFinal += " - " + request.getObservacion().trim();
        }

        registrarMovimiento(
                actualizado,
                presentacion.getId(),
                cantidadAjuste,
                tipoMovimiento,
                stockAnterior,
                cantidadBaseAjuste,
                stockNuevo,
                motivoFinal
        );

        return mapToResponse(actualizado);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR LOTE
    // ============================================================

    public LoteResponseDTO cambiarEstado(
            Integer id,
            Short nuevoEstado) {

        if (nuevoEstado == null
                || (!nuevoEstado.equals(EstadoRegistro.ACTIVO.getValor())
                && !nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor()))) {
            throw new BusinessException(
                    "Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)"
            );
        }

        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lote no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                lote.getEstado())) {
            throw new BusinessException(
                    "No se puede cambiar el estado de un lote eliminado"
            );
        }

        lote.setEstado(nuevoEstado);

        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            lote.setFechaDesactivacion(LocalDateTime.now());
        } else {
            lote.setFechaDesactivacion(null);
        }

        Lote actualizado = loteRepository.save(lote);

        return mapToResponse(actualizado);
    }

    // ============================================================
    // ELIMINACIÓN LÓGICA DEL LOTE
    // ============================================================

    public void eliminarLogicamente(Integer id) {

        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lote no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(
                lote.getEstado())) {
            throw new BusinessException(
                    "El lote ya está eliminado"
            );
        }

        BigDecimal cantidadAnterior = lote.getCantidad();

        lote.setEstado(EstadoRegistro.ELIMINADO.getValor());
        lote.setFechaEliminacion(LocalDateTime.now());
        lote.setCantidad(BigDecimal.ZERO);

        Lote eliminado = loteRepository.save(lote);

        // Registrar el retiro de existencias en el historial.
        if (cantidadAnterior.compareTo(BigDecimal.ZERO) > 0) {
            registrarMovimiento(
                    eliminado,
                    null,
                    null,
                    TipoMovimiento.MERMA,
                    cantidadAnterior,
                    cantidadAnterior.negate(),
                    BigDecimal.ZERO,
                    "Eliminación lógica de lote"
            );
        }
    }

    // ============================================================
    // CONSULTAS
    // ============================================================

    public List<LoteResponseDTO> listarLotes() {

        return loteRepository.listarTodos()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LoteResponseDTO> listarLotesPorProducto(
            Integer idProducto) {

        return loteRepository.listarPorProducto(idProducto)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public LoteResponseDTO obtenerLotePorId(Integer id) {

        Lote lote = loteRepository.findByIdActivo(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lote no encontrado con ID: " + id
                        )
                );

        return mapToResponse(lote);
    }

    public List<LoteResponseDTO> listarLotesPorVencer(int dias) {

        if (dias < 0) {
            throw new BusinessException(
                    "Los días de anticipación no pueden ser negativos"
            );
        }

        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);

        return loteRepository.listarPorVencer(hoy, limite)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LoteResponseDTO> listarLotesVencidos() {

        return loteRepository.listarVencidos(LocalDate.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // REGISTRAR MOVIMIENTO DE INVENTARIO
    // ============================================================

    private void registrarMovimiento(
            Lote lote,
            Integer idPresentacion,
            BigDecimal cantidadPresentacion,
            TipoMovimiento tipo,
            BigDecimal stockAnterior,
            BigDecimal cantidadBase,
            BigDecimal stockNuevo,
            String motivo) {

        Usuario usuarioActual = getUsuarioAutenticado();

        Presentacion presentacion = null;

        if (idPresentacion != null) {
            presentacion = presentacionRepository
                    .findById(idPresentacion)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Presentación no encontrada con ID: "
                                            + idPresentacion
                            )
                    );
        }

        MovimientoInventario movimiento = new MovimientoInventario();

        movimiento.setLote(lote);
        movimiento.setPresentacion(presentacion);
        movimiento.setUsuario(usuarioActual);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setCantidad(cantidadBase);
        movimiento.setCantidadPresentacion(cantidadPresentacion);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setMotivo(motivo);
        movimiento.setFecha(LocalDateTime.now());

        movimientoRepository.save(movimiento);
    }

    // ============================================================
    // USUARIO AUTENTICADO
    // ============================================================

    private Usuario getUsuarioAutenticado() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(
                    "Usuario no autenticado"
            );
        }

        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario autenticado no encontrado en la base de datos"
                        )
                );
    }

    // ============================================================
    // MAPEAR RESPUESTA
    // ============================================================

    private LoteResponseDTO mapToResponse(Lote lote) {

        return new LoteResponseDTO(
                lote.getId(),
                lote.getProducto().getId(),
                lote.getProducto().getNombre(),
                lote.getNumeroLote(),
                lote.getFechaVencimiento(),
                lote.getCantidad(),
                lote.getEstado(),
                lote.getFechaIngreso()
        );
    }

    // ============================================================
    // VALIDACIONES AUXILIARES
    // ============================================================

    private String normalizarNumeroLote(String numeroLote) {

        if (numeroLote == null) {
            return "";
        }

        return numeroLote.trim().replaceAll("\\s+", " ");
    }

    private void validarCantidadEntera(
            BigDecimal cantidad,
            String mensaje) {

        if (cantidad == null
                || cantidad.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(mensaje);
        }
    }
}


