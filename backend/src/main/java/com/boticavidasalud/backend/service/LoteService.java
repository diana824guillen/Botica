package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.DetalleIngresoDTO;
import com.boticavidasalud.backend.dto.LoteAjusteDTO;
import com.boticavidasalud.backend.dto.LotePresentacionInfoDTO;
import com.boticavidasalud.backend.dto.LoteRequestDTO;
import com.boticavidasalud.backend.dto.LoteResponseDTO;

import com.boticavidasalud.backend.entity.Lote;
import com.boticavidasalud.backend.entity.LotePresentacion;
import com.boticavidasalud.backend.entity.Presentacion;
import com.boticavidasalud.backend.entity.Producto;
import com.boticavidasalud.backend.entity.Usuario;

import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.enums.TipoMovimiento;

import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;

import com.boticavidasalud.backend.repository.LotePresentacionRepository;
import com.boticavidasalud.backend.repository.LoteRepository;
import com.boticavidasalud.backend.repository.MovimientoInventarioRepository;
import com.boticavidasalud.backend.repository.PresentacionRepository;
import com.boticavidasalud.backend.repository.ProductoRepository;
import com.boticavidasalud.backend.repository.UsuarioRepository;

import com.boticavidasalud.backend.entity.MovimientoInventario;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoteService {

    private final LoteRepository loteRepository;
    private final LotePresentacionRepository lotePresentacionRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PresentacionRepository presentacionRepository;

    public LoteService(
            LoteRepository loteRepository,
            LotePresentacionRepository lotePresentacionRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            MovimientoInventarioRepository movimientoRepository,
            PresentacionRepository presentacionRepository) {

        this.loteRepository = loteRepository;
        this.lotePresentacionRepository = lotePresentacionRepository;
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

        if (!EstadoRegistro.ACTIVO.getValor().equals(
                producto.getEstado())) {
            throw new BusinessException(
                    "Solo puede registrar lotes para productos activos"
            );
        }

        // Validar que no exista otro lote con el mismo número.
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

        BigDecimal cantidadTotalBase = BigDecimal.ZERO;

        Set<Integer> presentacionesProcesadas = new HashSet<>();
        Map<Integer, Presentacion> presentacionesIngreso = new HashMap<>();

        // 1. Validar presentaciones y calcular el total en unidad base.
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

            if (!EstadoRegistro.ACTIVO.getValor().equals(
                    presentacion.getEstado())) {
                throw new BusinessException(
                        "Solo puede registrar ingresos con presentaciones activas"
                );
            }

            Integer factorConversion = presentacion.getFactorConversion();

            if (factorConversion == null || factorConversion <= 0) {
                throw new BusinessException(
                        "El factor de conversión de la presentación no es válido"
                );
            }

            BigDecimal cantidadBase = cantidadPresentacion.multiply(
                    BigDecimal.valueOf(factorConversion)
            );

            cantidadTotalBase = cantidadTotalBase.add(cantidadBase);

            presentacionesIngreso.put(
                    presentacion.getId(),
                    presentacion
            );
        }

        // 2. Crear el lote con el stock total en unidad base.
        Lote lote = new Lote();

        lote.setProducto(producto);
        lote.setNumeroLote(numeroLote);
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setCantidad(cantidadTotalBase);
        lote.setEstado(EstadoRegistro.ACTIVO.getValor());

        Lote guardado = loteRepository.save(lote);

        // 3. Guardar el saldo real de cada presentación.
        BigDecimal stockAcumulado = BigDecimal.ZERO;

        for (DetalleIngresoDTO detalle : detalles) {

            Presentacion presentacion = presentacionesIngreso.get(
                    detalle.getIdPresentacion()
            );

            LotePresentacion saldo = new LotePresentacion();

            saldo.setLote(guardado);
            saldo.setPresentacion(presentacion);
            saldo.setCantidad(detalle.getCantidadPresentacion());

            lotePresentacionRepository.save(saldo);

            // 4. Registrar el movimiento expresado en unidad base.
            BigDecimal cantidadBase = detalle.getCantidadPresentacion()
                    .multiply(BigDecimal.valueOf(
                            presentacion.getFactorConversion()
                    ));

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

        Lote lote = loteRepository.findByIdForUpdate(id)
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

        // Buscar las existencias de esa presentación dentro del lote.
        LotePresentacion saldo = lotePresentacionRepository
                .findByLote_IdAndPresentacion_Id(
                        id,
                        presentacion.getId()
                )
                .orElse(null);

        // No permitir descontar una presentación que no tiene saldo.
        if (saldo == null
                && cantidadAjuste.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "No existen unidades disponibles de esta presentación en el lote"
            );
        }

        // Si el ajuste es positivo y no existe saldo, se crea.
        if (saldo == null) {
            saldo = new LotePresentacion();
            saldo.setLote(lote);
            saldo.setPresentacion(presentacion);
            saldo.setCantidad(BigDecimal.ZERO);
        }

        BigDecimal stockPresentacionAnterior = saldo.getCantidad();

        BigDecimal stockPresentacionNuevo =
                stockPresentacionAnterior.add(cantidadAjuste);

        if (stockPresentacionNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "No hay suficientes existencias de esta presentación en el lote"
            );
        }

        // Convertir el ajuste a unidad base para actualizar el lote.
        BigDecimal cantidadBaseAjuste = cantidadAjuste.multiply(
                BigDecimal.valueOf(factorConversion)
        );

        BigDecimal stockAnterior = lote.getCantidad() != null
                ? lote.getCantidad()
                : BigDecimal.ZERO;

        BigDecimal stockNuevo = stockAnterior.add(cantidadBaseAjuste);

        if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "El ajuste dejaría el stock total en negativo"
            );
        }

        // Actualizar el saldo por presentación.
        saldo.setCantidad(stockPresentacionNuevo);
        lotePresentacionRepository.save(saldo);

        // Actualizar el stock total en unidad base.
        lote.setCantidad(stockNuevo);
        Lote actualizado = loteRepository.save(lote);

        TipoMovimiento tipoMovimiento =
                request.getMotivo().getTipoMovimiento();

        String motivoFinal = request.getMotivo().getDescripcion();

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

        Lote lote = loteRepository.findByIdForUpdate(id)
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

        Lote lote = loteRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lote no encontrado con ID: " + id
                        )
                );

        if (EstadoRegistro.ELIMINADO.getValor().equals(lote.getEstado())) {
                throw new BusinessException("El lote ya está eliminado");
        }

        BigDecimal stockTotalAnterior = lote.getCantidad() != null
                ? lote.getCantidad()
                : BigDecimal.ZERO;

        List<LotePresentacion> saldos =
                lotePresentacionRepository.findByLote_IdOrderByIdAsc(id);

        BigDecimal stockRestante = stockTotalAnterior;

        for (LotePresentacion saldo : saldos) {

                BigDecimal cantidadPresentacion = saldo.getCantidad() != null
                        ? saldo.getCantidad()
                        : BigDecimal.ZERO;

                if (cantidadPresentacion.compareTo(BigDecimal.ZERO) > 0) {

                Presentacion presentacion = saldo.getPresentacion();

                BigDecimal cantidadBase = cantidadPresentacion.multiply(
                        BigDecimal.valueOf(presentacion.getFactorConversion())
                );

                BigDecimal stockNuevo = stockRestante.subtract(cantidadBase);

                if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
                        throw new BusinessException(
                                "Las existencias por presentación no coinciden "
                                + "con el stock total del lote. Revise el inventario."
                        );
                }

                registrarMovimiento(
                        lote,
                        presentacion.getId(),
                        cantidadPresentacion.negate(),
                        TipoMovimiento.MERMA,
                        stockRestante,
                        cantidadBase.negate(),
                        stockNuevo,
                        "Eliminación lógica de lote"
                );

                stockRestante = stockNuevo;
                }

                saldo.setCantidad(BigDecimal.ZERO);
        }

        lotePresentacionRepository.saveAll(saldos);

        // Si quedó una diferencia, se registra como retiro general.
        if (stockRestante.compareTo(BigDecimal.ZERO) > 0) {

                registrarMovimiento(
                        lote,
                        null,
                        null,
                        TipoMovimiento.MERMA,
                        stockRestante,
                        stockRestante.negate(),
                        BigDecimal.ZERO,
                        "Retiro de diferencia al eliminar lote"
                );
        }

        lote.setEstado(EstadoRegistro.ELIMINADO.getValor());
        lote.setFechaEliminacion(LocalDateTime.now());
        lote.setCantidad(BigDecimal.ZERO);

        loteRepository.save(lote);
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

        List<LotePresentacionInfoDTO> existencias =
                lotePresentacionRepository
                        .findByLote_IdOrderByIdAsc(lote.getId())
                        .stream()
                        .map(saldo -> new LotePresentacionInfoDTO(
                                saldo.getPresentacion().getId(),
                                saldo.getPresentacion().getTipo(),
                                saldo.getPresentacion().getFactorConversion(),
                                saldo.getCantidad()
                        ))
                        .collect(Collectors.toList());

        return new LoteResponseDTO(
                lote.getId(),
                lote.getProducto().getId(),
                lote.getProducto().getNombre(),
                lote.getNumeroLote(),
                lote.getFechaVencimiento(),
                lote.getCantidad(),
                existencias,
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


