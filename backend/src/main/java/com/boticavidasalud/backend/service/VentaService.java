package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.*;
import com.boticavidasalud.backend.entity.*;
import com.boticavidasalud.backend.enums.EstadoRegistro;
import com.boticavidasalud.backend.enums.MetodoPago;
import com.boticavidasalud.backend.enums.TipoCondicionDescuento;
import com.boticavidasalud.backend.enums.TipoMovimiento;
import com.boticavidasalud.backend.exception.BusinessException;
import com.boticavidasalud.backend.exception.ResourceNotFoundException;
import com.boticavidasalud.backend.repository.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
@Transactional
public class VentaService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int ESCALA_MONETARIA = 2;

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PresentacionRepository presentacionRepository;
    private final LoteRepository loteRepository;
    private final LotePresentacionRepository lotePresentacionRepository;
    private final DescuentoRepository descuentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public VentaService(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            PresentacionRepository presentacionRepository,
            LoteRepository loteRepository,
            LotePresentacionRepository lotePresentacionRepository,
            DescuentoRepository descuentoRepository,
            MovimientoInventarioRepository movimientoRepository) {

        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.presentacionRepository = presentacionRepository;
        this.loteRepository = loteRepository;
        this.lotePresentacionRepository = lotePresentacionRepository;
        this.descuentoRepository = descuentoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // ============================================================
    // PREVISUALIZAR VENTA
    // ============================================================

    @Transactional(readOnly = true)
    public VentaPrevisualizacionResponseDTO previsualizarVenta(
            VentaPrevisualizacionRequestDTO request) {

        if (request == null
                || request.getDetalles() == null
                || request.getDetalles().isEmpty()) {
            throw new BusinessException(
                    "Debe agregar al menos un producto al carrito"
            );
        }

        List<LineaVenta> lineas = prepararLineasVenta(
                request.getDetalles()
        );

        ResumenVenta resumen = calcularResumenVenta(lineas);

        VentaPrevisualizacionResponseDTO response =
                new VentaPrevisualizacionResponseDTO();

        response.setSubtotal(resumen.subtotal);
        response.setDescuentoTotal(resumen.descuentoTotal);
        response.setTotal(resumen.total);

        List<VentaPrevisualizacionResponseDTO.DetalleInfoDTO>
                detallesResponse = new ArrayList<>();

        boolean todasLasPresentacionesTienenStock = true;

        for (LineaVenta linea : lineas) {

            BigDecimal stockDisponible =
                    lotePresentacionRepository.sumarStockDisponible(
                            linea.presentacion.getId(),
                            LocalDate.now()
                    );

            if (stockDisponible == null) {
                stockDisponible = BigDecimal.ZERO;
            }

            boolean tieneStockSuficiente =
                    stockDisponible.compareTo(
                            linea.cantidadPresentacion
                    ) >= 0;

            if (!tieneStockSuficiente) {
                todasLasPresentacionesTienenStock = false;
            }

            VentaPrevisualizacionResponseDTO.DetalleInfoDTO detalleResponse =
                    new VentaPrevisualizacionResponseDTO.DetalleInfoDTO();

            detalleResponse.setIdProducto(linea.producto.getId());
            detalleResponse.setIdPresentacion(linea.presentacion.getId());
            detalleResponse.setProductoNombre(linea.producto.getNombre());
            detalleResponse.setPresentacionTipo(
                    linea.presentacion.getTipo().name()
            );
            detalleResponse.setFactorConversion(linea.factorConversion);
            detalleResponse.setCantidad(linea.cantidadPresentacion);
            detalleResponse.setPrecioUnitario(linea.precioUnitario);
            detalleResponse.setSubtotal(linea.subtotal);
            detalleResponse.setStockDisponible(stockDisponible);
            detalleResponse.setStockSuficiente(tieneStockSuficiente);

            detallesResponse.add(detalleResponse);
        }

        response.setDetalles(detallesResponse);

        List<VentaPrevisualizacionResponseDTO.DescuentoInfoDTO>
                descuentosResponse = new ArrayList<>();

        for (DescuentoAplicado descuento : resumen.descuentosAplicados) {

            VentaPrevisualizacionResponseDTO.DescuentoInfoDTO info =
                    new VentaPrevisualizacionResponseDTO.DescuentoInfoDTO();

            info.setIdDescuento(descuento.idDescuento);
            info.setNombre(descuento.nombre);
            info.setProductoNombre(descuento.productoNombre);
            info.setTipoCondicion(descuento.tipoCondicion);
            info.setValorCondicion(descuento.valorCondicion);
            info.setPorcentaje(descuento.porcentaje);
            info.setMontoDescuento(descuento.montoDescuento);

            descuentosResponse.add(info);
        }

        response.setDescuentosAplicados(descuentosResponse);
        response.setStockSuficiente(todasLasPresentacionesTienenStock);

        return response;
    }

    // ============================================================
    // REGISTRAR VENTA
    // ============================================================

    public VentaResponseDTO registrarVenta(VentaRequestDTO request) {

        if (request == null) {
            throw new BusinessException(
                    "Los datos de la venta son obligatorios"
            );
        }

        if (request.getDetalles() == null
                || request.getDetalles().isEmpty()) {
            throw new BusinessException(
                    "La venta debe tener al menos un producto"
            );
        }

        if (request.getObservacion() != null
                && request.getObservacion().length() > 255) {
            throw new BusinessException(
                    "La observación no puede superar los 255 caracteres"
            );
        }

        Usuario cajero = getUsuarioAutenticado();

        Cliente cliente = null;

        if (request.getIdCliente() != null) {
            cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Cliente no encontrado"
                            )
                    );
        }

        List<LineaVenta> lineas = prepararLineasVenta(
                request.getDetalles()
        );

        ResumenVenta resumen = calcularResumenVenta(lineas);

        // Validar pagos antes de modificar el inventario.
        List<PagoRequestDTO> pagosValidos = validarPagos(
                request.getPagos(),
                resumen.total
        );

        Venta venta = new Venta();

        venta.setCliente(cliente);
        venta.setUsuario(cajero);
        venta.setObservacion(request.getObservacion());
        venta.setNumeroNotaVenta(generarCorrelativo());
        venta.setEstado(EstadoRegistro.ACTIVO.getValor());
        venta.setDescuentoTotal(resumen.descuentoTotal);
        venta.setTotal(resumen.total);

        // Descontar existencias por presentación y lote.
        List<DetalleVenta> detallesVenta = descontarExistencias(
                venta,
                lineas,
                cajero
        );

        venta.setDetalles(detallesVenta);

        // Registrar los pagos y calcular el vuelto en el backend.
        List<PagoVenta> pagosVenta = new ArrayList<>();

        for (PagoRequestDTO pagoRequest : pagosValidos) {

            PagoVenta pago = new PagoVenta();

            pago.setVenta(venta);
            pago.setMetodoPago(pagoRequest.getMetodoPago());
            pago.setMonto(pagoRequest.getMonto());
            pago.setFechaPago(LocalDateTime.now());
            pago.setEstado(EstadoRegistro.ACTIVO.getValor());

            if (pagoRequest.getMetodoPago() == MetodoPago.EFECTIVO) {

                BigDecimal montoRecibido = pagoRequest.getMontoRecibido();

                pago.setMontoRecibido(montoRecibido);

                BigDecimal vuelto = montoRecibido
                        .subtract(pagoRequest.getMonto())
                        .setScale(
                                ESCALA_MONETARIA,
                                RoundingMode.HALF_UP
                        );

                pago.setVuelto(vuelto);

            } else {

                // Los métodos no monetarios no generan vuelto.
                pago.setMontoRecibido(null);
                pago.setVuelto(BigDecimal.ZERO.setScale(
                        ESCALA_MONETARIA,
                        RoundingMode.HALF_UP
                ));
            }

            pagosVenta.add(pago);
        }

        venta.setPagos(pagosVenta);

        // Guardar venta, detalles y pagos en una sola transacción.
        Venta ventaGuardada = ventaRepository.save(venta);

        return mapToResponse(ventaGuardada, resumen.subtotal);
    }

    // ============================================================
    // PREPARAR LÍNEAS DE VENTA
    // ============================================================

    private List<LineaVenta> prepararLineasVenta(
            List<DetalleVentaRequestDTO> detallesRequest) {

        Map<Integer, BigDecimal> cantidadesPorPresentacion =
                new TreeMap<>();

        for (DetalleVentaRequestDTO detalle : detallesRequest) {

            if (detalle == null
                    || detalle.getIdPresentacion() == null
                    || detalle.getCantidad() == null) {
                throw new BusinessException(
                        "Los datos de cada producto de la venta son obligatorios"
                );
            }

            BigDecimal cantidad = detalle.getCantidad();

            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "La cantidad debe ser mayor a cero"
                );
            }

            validarCantidadEntera(
                    cantidad,
                    "La cantidad de venta debe ser un número entero"
            );

            acumularValor(
                    cantidadesPorPresentacion,
                    detalle.getIdPresentacion(),
                    cantidad
            );
        }

        List<LineaVenta> lineas = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> entrada
                : cantidadesPorPresentacion.entrySet()) {

            Presentacion presentacion = presentacionRepository
                    .findById(entrada.getKey())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Presentación no encontrada con ID: "
                                            + entrada.getKey()
                            )
                    );

            Producto producto = presentacion.getProducto();

            if (producto == null
                    || !EstadoRegistro.ACTIVO.getValor().equals(
                            producto.getEstado())) {
                throw new BusinessException(
                        "El producto seleccionado no está activo"
                );
            }

            if (!EstadoRegistro.ACTIVO.getValor().equals(
                    presentacion.getEstado())) {
                throw new BusinessException(
                        "La presentación seleccionada no está activa"
                );
            }

            Integer factorConversion =
                    presentacion.getFactorConversion();

            if (factorConversion == null || factorConversion <= 0) {
                throw new BusinessException(
                        "El factor de conversión de la presentación no es válido"
                );
            }

            BigDecimal precio = presentacion.getPrecioVenta();

            if (precio == null
                    || precio.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "El precio de venta de la presentación no es válido"
                );
            }

            BigDecimal cantidad = entrada.getValue();

            BigDecimal subtotalLinea = cantidad
                    .multiply(precio)
                    .setScale(
                            ESCALA_MONETARIA,
                            RoundingMode.HALF_UP
                    );

            LineaVenta linea = new LineaVenta();

            linea.presentacion = presentacion;
            linea.producto = producto;
            linea.cantidadPresentacion = cantidad;
            linea.factorConversion = factorConversion;
            linea.precioUnitario = precio;
            linea.subtotal = subtotalLinea;

            lineas.add(linea);
        }

        return lineas;
    }

    // ============================================================
    // CALCULAR SUBTOTAL Y DESCUENTOS
    // ============================================================

    private ResumenVenta calcularResumenVenta(
            List<LineaVenta> lineas) {

        BigDecimal subtotal = BigDecimal.ZERO;

        Map<Integer, BigDecimal> subtotalPorProducto =
                new HashMap<>();

        Map<Integer, BigDecimal> cantidadBasePorProducto =
                new HashMap<>();

        for (LineaVenta linea : lineas) {

            Integer idProducto = linea.producto.getId();

            subtotal = subtotal.add(linea.subtotal);

            acumularValor(
                    subtotalPorProducto,
                    idProducto,
                    linea.subtotal
            );

            BigDecimal cantidadBase = linea.cantidadPresentacion
                    .multiply(
                            BigDecimal.valueOf(linea.factorConversion)
                    );

            acumularValor(
                    cantidadBasePorProducto,
                    idProducto,
                    cantidadBase
            );
        }

        subtotal = subtotal.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );

        List<Descuento> descuentos =
                new ArrayList<>(
                        descuentoRepository.findDescuentosVigentes(
                                LocalDate.now()
                        )
                );

        descuentos.sort(
                (primero, segundo) ->
                        Integer.compare(
                                primero.getId(),
                                segundo.getId()
                        )
        );

        Map<Integer, BigDecimal> descuentosPorProducto =
                new HashMap<>();

        List<DescuentoAplicado> descuentosAplicados =
                new ArrayList<>();

        for (Descuento descuento : descuentos) {

            if (descuento.getProducto() == null
                    || descuento.getTipoCondicion() == null
                    || descuento.getPorcentaje() == null
                    || descuento.getValorCondicion() == null) {
                continue;
            }

            Integer idProducto = descuento.getProducto().getId();

            BigDecimal subtotalProducto =
                    subtotalPorProducto.get(idProducto);

            if (subtotalProducto == null
                    || subtotalProducto.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            boolean cumpleCondicion = false;

            if (descuento.getTipoCondicion()
                    == TipoCondicionDescuento.MONTO_MINIMO) {

                cumpleCondicion = subtotal.compareTo(
                        descuento.getValorCondicion()
                ) >= 0;

            } else if (descuento.getTipoCondicion()
                    == TipoCondicionDescuento.CANTIDAD_MINIMA) {

                BigDecimal cantidadBaseProducto =
                        cantidadBasePorProducto.get(idProducto);

                if (cantidadBaseProducto == null) {
                    cantidadBaseProducto = BigDecimal.ZERO;
                }

                cumpleCondicion = cantidadBaseProducto.compareTo(
                        descuento.getValorCondicion()
                ) >= 0;
            }

            if (!cumpleCondicion) {
                continue;
            }

            BigDecimal porcentaje = descuento.getPorcentaje();

            if (porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal descuentoPropuesto = subtotalProducto
                    .multiply(porcentaje)
                    .divide(
                            CIEN,
                            ESCALA_MONETARIA,
                            RoundingMode.HALF_UP
                    );

            BigDecimal descuentoAcumulado =
                    descuentosPorProducto.get(idProducto);

            if (descuentoAcumulado == null) {
                descuentoAcumulado = BigDecimal.ZERO;
            }

            BigDecimal descuentoRestante = subtotalProducto
                    .subtract(descuentoAcumulado)
                    .max(BigDecimal.ZERO);

            BigDecimal descuentoAplicado = descuentoPropuesto
                    .min(descuentoRestante);

            if (descuentoAplicado.compareTo(BigDecimal.ZERO) > 0) {

                acumularValor(
                        descuentosPorProducto,
                        idProducto,
                        descuentoAplicado
                );

                DescuentoAplicado aplicado = new DescuentoAplicado();

                aplicado.idDescuento = descuento.getId();
                aplicado.nombre = descuento.getNombre();
                aplicado.productoNombre =
                        descuento.getProducto().getNombre();
                aplicado.tipoCondicion =
                        descuento.getTipoCondicion();
                aplicado.valorCondicion =
                        descuento.getValorCondicion();
                aplicado.porcentaje = porcentaje;
                aplicado.montoDescuento = descuentoAplicado;

                descuentosAplicados.add(aplicado);
            }
        }

        BigDecimal descuentoTotal = BigDecimal.ZERO;

        for (BigDecimal descuento : descuentosPorProducto.values()) {
            if (descuento != null) {
                descuentoTotal = descuentoTotal.add(descuento);
            }
        }

        descuentoTotal = descuentoTotal.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );

        BigDecimal total = subtotal
                .subtract(descuentoTotal)
                .max(BigDecimal.ZERO)
                .setScale(
                        ESCALA_MONETARIA,
                        RoundingMode.HALF_UP
                );

        return new ResumenVenta(
                subtotal,
                descuentoTotal,
                total,
                descuentosAplicados
        );
    }

    // ============================================================
    // VALIDAR PAGOS
    // ============================================================

    private List<PagoRequestDTO> validarPagos(
            List<PagoRequestDTO> pagosRequest,
            BigDecimal totalVenta) {

        List<PagoRequestDTO> pagos = pagosRequest != null
                ? pagosRequest
                : List.of();

        if (totalVenta.compareTo(BigDecimal.ZERO) == 0) {

            if (!pagos.isEmpty()) {
                throw new BusinessException(
                        "La venta tiene total cero. No debe registrar pagos."
                );
            }

            return pagos;
        }

        if (pagos.isEmpty()) {
            throw new BusinessException(
                    "Debe registrar al menos un método de pago"
            );
        }

        BigDecimal totalPagado = BigDecimal.ZERO;

        for (PagoRequestDTO pago : pagos) {

            if (pago == null
                    || pago.getMetodoPago() == null
                    || pago.getMonto() == null) {
                throw new BusinessException(
                        "Los datos de cada pago son obligatorios"
                );
            }

            if (pago.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "El monto de cada pago debe ser mayor a cero"
                );
            }

            if (pago.getMonto().stripTrailingZeros().scale() > 2) {
                throw new BusinessException(
                        "El monto del pago admite máximo dos decimales"
                );
            }

            // Validaciones específicas para efectivo.
            if (pago.getMetodoPago() == MetodoPago.EFECTIVO) {

                BigDecimal recibido = pago.getMontoRecibido();

                if (recibido == null) {
                    throw new BusinessException(
                            "Debe indicar el monto recibido para el pago en efectivo"
                    );
                }

                if (recibido.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(
                            "El monto recibido debe ser mayor a cero"
                    );
                }

                if (recibido.stripTrailingZeros().scale() > 2) {
                    throw new BusinessException(
                            "El monto recibido admite máximo dos decimales"
                    );
                }

                if (recibido.compareTo(pago.getMonto()) < 0) {
                    throw new BusinessException(
                            "El monto recibido en efectivo no puede ser menor al importe aplicado"
                    );
                }

            } else {

                // Los métodos distintos al efectivo no reciben vuelto.
                if (pago.getMontoRecibido() != null) {
                    throw new BusinessException(
                            "El monto recibido solo debe indicarse para pagos en efectivo"
                    );
                }
            }

            totalPagado = totalPagado.add(pago.getMonto());
        }

        totalPagado = totalPagado.setScale(
                ESCALA_MONETARIA,
                RoundingMode.HALF_UP
        );

        if (totalPagado.compareTo(totalVenta) != 0) {
            throw new BusinessException(
                    "El monto pagado (" + totalPagado
                            + ") no coincide con el total de la venta ("
                            + totalVenta + ")"
            );
        }

        return pagos;
    }

    // ============================================================
    // BLOQUEAR LOTES CANDIDATOS PARA LA VENTA
    // ============================================================

    private Map<Integer, Lote> bloquearLotesCandidatos(
            List<LineaVenta> lineas) {

        LocalDate hoy = LocalDate.now();

        TreeSet<Integer> idsLotes = new TreeSet<>();

        for (LineaVenta linea : lineas) {

            List<Integer> idsEncontrados =
                    lotePresentacionRepository
                            .findIdsLotesDisponiblesParaVenta(
                                    linea.presentacion.getId(),
                                    hoy
                            );

            idsLotes.addAll(idsEncontrados);
        }

        Map<Integer, Lote> lotesBloqueados = new TreeMap<>();

        for (Integer idLote : idsLotes) {

            Optional<Lote> loteOptional =
                    loteRepository.findByIdForUpdate(idLote);

            if (loteOptional.isEmpty()) {
                continue;
            }

            Lote lote = loteOptional.get();

            if (!EstadoRegistro.ACTIVO.getValor().equals(
                    lote.getEstado())) {
                continue;
            }

            if (lote.getFechaVencimiento() == null
                    || lote.getFechaVencimiento().isBefore(hoy)) {
                continue;
            }

            if (lote.getCantidad() == null
                    || lote.getCantidad().compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {
                continue;
            }

            lotesBloqueados.put(idLote, lote);
        }

        return lotesBloqueados;
    }

    // ============================================================
    // DESCONTAR EXISTENCIAS POR PRESENTACIÓN
    // ============================================================

    private List<DetalleVenta> descontarExistencias(
            Venta venta,
            List<LineaVenta> lineas,
            Usuario cajero) {

        List<DetalleVenta> detallesVenta = new ArrayList<>();

        Map<Integer, Lote> lotesBloqueados =
                bloquearLotesCandidatos(lineas);

        List<Integer> idsLotesBloqueados =
                new ArrayList<>(lotesBloqueados.keySet());

        LocalDate hoy = LocalDate.now();

        for (LineaVenta linea : lineas) {

            Integer idPresentacion = linea.presentacion.getId();

            if (idsLotesBloqueados.isEmpty()) {
                throw new BusinessException(
                        "No hay stock disponible de "
                                + linea.presentacion.getTipo()
                                + " para el producto: "
                                + linea.producto.getNombre()
                );
            }

            List<LotePresentacion> saldos =
                    lotePresentacionRepository
                            .findSaldosDisponiblesParaVentaForUpdate(
                                    idPresentacion,
                                    hoy,
                                    idsLotesBloqueados
                            );

            BigDecimal stockDisponiblePresentacion = BigDecimal.ZERO;

            for (LotePresentacion saldo : saldos) {
                if (saldo.getCantidad() != null) {
                    stockDisponiblePresentacion =
                            stockDisponiblePresentacion.add(
                                    saldo.getCantidad()
                            );
                }
            }

            if (stockDisponiblePresentacion.compareTo(
                    linea.cantidadPresentacion
            ) < 0) {

                throw new BusinessException(
                        "Stock insuficiente de "
                                + linea.presentacion.getTipo()
                                + " para el producto "
                                + linea.producto.getNombre()
                                + ". Disponible: "
                                + stockDisponiblePresentacion
                                + ", solicitado: "
                                + linea.cantidadPresentacion
                );
            }

            BigDecimal cantidadRestante =
                    linea.cantidadPresentacion;

            BigDecimal factor = BigDecimal.valueOf(
                    linea.factorConversion
            );

            for (LotePresentacion saldo : saldos) {

                if (cantidadRestante.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {
                    break;
                }

                BigDecimal stockPresentacion = saldo.getCantidad();

                if (stockPresentacion == null
                        || stockPresentacion.compareTo(
                                BigDecimal.ZERO
                        ) <= 0) {
                    continue;
                }

                BigDecimal cantidadADescontar =
                        cantidadRestante.min(stockPresentacion);

                validarCantidadEntera(
                        cantidadADescontar,
                        "El stock de la presentación contiene cantidades "
                                + "no enteras. Revise el inventario del lote."
                );

                Integer idLote = saldo.getLote().getId();

                Lote lote = lotesBloqueados.get(idLote);

                if (lote == null) {
                    throw new BusinessException(
                            "No se pudo bloquear el lote para completar la venta"
                    );
                }

                BigDecimal cantidadBaseADescontar =
                        cantidadADescontar.multiply(factor);

                BigDecimal stockAnterior = lote.getCantidad() != null
                        ? lote.getCantidad()
                        : BigDecimal.ZERO;

                BigDecimal stockNuevo = stockAnterior.subtract(
                        cantidadBaseADescontar
                );

                if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException(
                            "El stock total del lote no coincide con "
                                    + "las existencias de sus presentaciones. "
                                    + "Revise el lote: " + lote.getNumeroLote()
                    );
                }

                BigDecimal nuevoStockPresentacion =
                        stockPresentacion.subtract(cantidadADescontar);

                saldo.setCantidad(nuevoStockPresentacion);
                lotePresentacionRepository.save(saldo);

                lote.setCantidad(stockNuevo);
                loteRepository.save(lote);

                DetalleVenta detalleVenta = new DetalleVenta();

                detalleVenta.setVenta(venta);
                detalleVenta.setPresentacion(linea.presentacion);
                detalleVenta.setLote(lote);
                detalleVenta.setCantidad(cantidadBaseADescontar);
                detalleVenta.setPrecioUnitario(linea.precioUnitario);
                detalleVenta.setEstado(
                        EstadoRegistro.ACTIVO.getValor()
                );

                detallesVenta.add(detalleVenta);

                MovimientoInventario movimiento =
                        new MovimientoInventario();

                movimiento.setLote(lote);
                movimiento.setPresentacion(linea.presentacion);
                movimiento.setUsuario(cajero);
                movimiento.setTipoMovimiento(
                        TipoMovimiento.SALIDA_VENTA
                );

                movimiento.setStockAnterior(stockAnterior);
                movimiento.setCantidad(
                        cantidadBaseADescontar.negate()
                );
                movimiento.setCantidadPresentacion(
                        cantidadADescontar.negate()
                );
                movimiento.setStockNuevo(stockNuevo);

                movimiento.setMotivo(
                        "Venta " + venta.getNumeroNotaVenta()
                );

                movimiento.setFecha(LocalDateTime.now());

                movimientoRepository.save(movimiento);

                cantidadRestante = cantidadRestante.subtract(
                        cantidadADescontar
                );
            }

            if (cantidadRestante.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(
                        "No se pudo completar la salida del producto: "
                                + linea.producto.getNombre()
                );
            }
        }

        return detallesVenta;
    }

    // ============================================================
    // ACUMULAR VALORES EN MAPAS
    // ============================================================

    private void acumularValor(
            Map<Integer, BigDecimal> mapa,
            Integer clave,
            BigDecimal cantidad) {

        BigDecimal valorActual = mapa.get(clave);

        if (valorActual == null) {
            valorActual = BigDecimal.ZERO;
        }

        mapa.put(
                clave,
                valorActual.add(cantidad)
        );
    }

    // ============================================================
    // HISTORIAL DE COMPRAS DE UN CLIENTE
    // ============================================================

    public List<HistorialCompraDTO> obtenerHistorialCompras(
            Integer idCliente) {

        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException(
                    "Cliente no encontrado"
            );
        }

        List<Venta> ventas =
                ventaRepository.findByClienteId(idCliente);

        return ventas.stream()
                .map(v -> new HistorialCompraDTO(
                        v.getId(),
                        v.getNumeroNotaVenta(),
                        v.getFechaVenta(),
                        v.getTotal(),
                        v.getEstado()
                ))
                .toList();
    }

    // ============================================================
    // VALIDAR CANTIDADES ENTERAS
    // ============================================================

    private void validarCantidadEntera(
            BigDecimal cantidad,
            String mensaje) {

        if (cantidad == null
                || cantidad.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(mensaje);
        }
    }

    // ============================================================
    // GENERAR CORRELATIVO
    // ============================================================

    private String generarCorrelativo() {

        Optional<Venta> ultimaVenta =
                ventaRepository.findUltimaVenta();

        if (ultimaVenta.isPresent()) {

            String ultimoNumero =
                    ultimaVenta.get().getNumeroNotaVenta();

            try {
                int numero = Integer.parseInt(
                        ultimoNumero.substring(3)
                ) + 1;

                return String.format("NV-%06d", numero);

            } catch (RuntimeException ex) {
                throw new BusinessException(
                        "No se pudo generar el número correlativo de la venta"
                );
            }
        }

        return "NV-000001";
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
                                "Usuario autenticado no encontrado"
                        )
                );
    }

    // ============================================================
    // MAPEAR RESPUESTA DE VENTA
    // ============================================================

    private VentaResponseDTO mapToResponse(
            Venta venta,
            BigDecimal subtotal) {

        VentaResponseDTO dto = new VentaResponseDTO();

        dto.setId(venta.getId());
        dto.setNumeroNotaVenta(venta.getNumeroNotaVenta());
        dto.setFechaVenta(venta.getFechaVenta());

        dto.setClienteNombre(
                venta.getCliente() != null
                        ? venta.getCliente().getNombres()
                        : "Cliente Varios"
        );

        dto.setCajeroNombre(venta.getUsuario().getNombres());

        dto.setSubtotal(subtotal);
        dto.setDescuentoTotal(venta.getDescuentoTotal());
        dto.setTotal(venta.getTotal());

        List<VentaResponseDTO.DetalleVentaInfoDTO> detallesDto =
                new ArrayList<>();

        for (DetalleVenta detalle : venta.getDetalles()) {

            VentaResponseDTO.DetalleVentaInfoDTO info =
                    new VentaResponseDTO.DetalleVentaInfoDTO();

            Presentacion presentacion = detalle.getPresentacion();

            Integer factorConversion =
                    presentacion.getFactorConversion();

            BigDecimal cantidadBase = detalle.getCantidad();

            BigDecimal cantidadPresentacion = cantidadBase.divide(
                    BigDecimal.valueOf(factorConversion),
                    ESCALA_MONETARIA,
                    RoundingMode.HALF_UP
            );

            info.setProductoNombre(
                    presentacion.getProducto().getNombre()
            );

            info.setPresentacionTipo(
                    presentacion.getTipo().name()
            );

            info.setCantidad(cantidadPresentacion);
            info.setFactorConversion(factorConversion);
            info.setCantidadBase(cantidadBase);

            info.setPrecioUnitario(
                    detalle.getPrecioUnitario()
            );

            BigDecimal subtotalDetalle = cantidadPresentacion
                    .multiply(detalle.getPrecioUnitario())
                    .setScale(
                            ESCALA_MONETARIA,
                            RoundingMode.HALF_UP
                    );

            info.setSubtotal(subtotalDetalle);

            info.setNumeroLote(
                    detalle.getLote().getNumeroLote()
            );

            detallesDto.add(info);
        }

        dto.setDetalles(detallesDto);

        List<VentaResponseDTO.PagoInfoDTO> pagosDto =
                new ArrayList<>();

        for (PagoVenta pago : venta.getPagos()) {

            VentaResponseDTO.PagoInfoDTO info =
                    new VentaResponseDTO.PagoInfoDTO();

            info.setMetodoPago(pago.getMetodoPago());
            info.setMonto(pago.getMonto());
            info.setMontoRecibido(pago.getMontoRecibido());
            info.setVuelto(pago.getVuelto());

            pagosDto.add(info);
        }

        dto.setPagos(pagosDto);

        return dto;
    }

    // ============================================================
    // CLASE AUXILIAR: LÍNEA DE VENTA
    // ============================================================

    private static class LineaVenta {

        private Presentacion presentacion;
        private Producto producto;

        private BigDecimal cantidadPresentacion;
        private Integer factorConversion;

        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }

    // ============================================================
    // CLASE AUXILIAR: DESCUENTO APLICADO
    // ============================================================

    private static class DescuentoAplicado {

        private Integer idDescuento;
        private String nombre;
        private String productoNombre;

        private TipoCondicionDescuento tipoCondicion;
        private BigDecimal valorCondicion;
        private BigDecimal porcentaje;

        private BigDecimal montoDescuento;
    }

    // ============================================================
    // CLASE AUXILIAR: RESUMEN DE VENTA
    // ============================================================

    private static class ResumenVenta {

        private final BigDecimal subtotal;
        private final BigDecimal descuentoTotal;
        private final BigDecimal total;

        private final List<DescuentoAplicado> descuentosAplicados;

        private ResumenVenta(
                BigDecimal subtotal,
                BigDecimal descuentoTotal,
                BigDecimal total,
                List<DescuentoAplicado> descuentosAplicados) {

            this.subtotal = subtotal;
            this.descuentoTotal = descuentoTotal;
            this.total = total;
            this.descuentosAplicados = descuentosAplicados;
        }
    }
}


