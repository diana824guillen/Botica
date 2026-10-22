package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.*;
import com.boticavidasalud.backend.entity.*;
import com.boticavidasalud.backend.enums.EstadoRegistro;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PresentacionRepository presentacionRepository;
    private final LoteRepository loteRepository;
    private final DescuentoRepository descuentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public VentaService(VentaRepository ventaRepository, ClienteRepository clienteRepository,
                        UsuarioRepository usuarioRepository, PresentacionRepository presentacionRepository,
                        LoteRepository loteRepository, DescuentoRepository descuentoRepository,
                        MovimientoInventarioRepository movimientoRepository) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.presentacionRepository = presentacionRepository;
        this.loteRepository = loteRepository;
        this.descuentoRepository = descuentoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public VentaResponseDTO registrarVenta(VentaRequestDTO request) {
        // 1. Validar Usuario y Cliente
        Usuario cajero = getUsuarioAutenticado();
        Cliente cliente = null;
        if (request.getIdCliente() != null) {
            cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        }

        // 2. Preparar la Venta
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setUsuario(cajero);
        venta.setObservacion(request.getObservacion());
        venta.setNumeroNotaVenta(generarCorrelativo());
        venta.setEstado(EstadoRegistro.ACTIVO.getValor());
        
        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetalleVenta> detallesVenta = new ArrayList<>();
        
        // 3. Procesar Detalles (Validar Stock y aplicar FIFO)
        for (DetalleVentaRequestDTO itemReq : request.getDetalles()) {
            Presentacion pres = presentacionRepository.findById(itemReq.getIdPresentacion())
                    .orElseThrow(() -> new ResourceNotFoundException("Presentación no encontrada"));

            Producto prod = pres.getProducto();
            BigDecimal cantidadBaseNecesaria = itemReq.getCantidad().multiply(BigDecimal.valueOf(pres.getFactorConversion()));
            
            // Calcular precio de este item (en base a la presentación)
            BigDecimal precioUnitario = pres.getPrecioVenta();
            BigDecimal subtotalItem = precioUnitario.multiply(itemReq.getCantidad());
            subtotal = subtotal.add(subtotalItem);

            // Lógica FIFO: Buscar lotes por orden de vencimiento
            List<Lote> lotes = loteRepository.findLotesActivosParaVenta(prod.getId());
            BigDecimal stockRestantePorAsignar = cantidadBaseNecesaria;

            for (Lote lote : lotes) {
                if (stockRestantePorAsignar.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal stockDisponible = lote.getCantidad();
                BigDecimal cantidadADescontar = stockRestantePorAsignar.min(stockDisponible);

                // Actualizar Lote
                lote.setCantidad(lote.getCantidad().subtract(cantidadADescontar));
                loteRepository.save(lote);

                // Crear Detalle de Venta (puede haber varios detalles para un mismo producto si se usaron varios lotes)
                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(venta);
                detalle.setPresentacion(pres);
                detalle.setLote(lote);
                // La cantidad en el detalle se guarda en unidades base que salieron de ese lote
                detalle.setCantidad(cantidadADescontar); 
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setEstado(EstadoRegistro.ACTIVO.getValor());
                detallesVenta.add(detalle);

                // Registrar Movimiento de SALIDA_VENTA
                MovimientoInventario mov = new MovimientoInventario();
                mov.setLote(lote);
                mov.setPresentacion(pres);
                mov.setUsuario(cajero);
                mov.setTipoMovimiento(TipoMovimiento.SALIDA_VENTA);
                mov.setStockAnterior(lote.getCantidad().add(cantidadADescontar));
                mov.setCantidad(cantidadADescontar.negate()); 
                mov.setStockNuevo(lote.getCantidad());
                mov.setMotivo("Venta");
                mov.setFecha(LocalDateTime.now());
                movimientoRepository.save(mov);

                stockRestantePorAsignar = stockRestantePorAsignar.subtract(cantidadADescontar);
            }

            if (stockRestantePorAsignar.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("Stock insuficiente para el producto: " + prod.getNombre());
            }
        }
        
        venta.setDetalles(detallesVenta);

        // 4. Aplicar Descuentos
        BigDecimal descuentoTotal = calcularDescuentos(request.getDetalles(), subtotal);
        venta.setDescuentoTotal(descuentoTotal);
        
        BigDecimal totalFinal = subtotal.subtract(descuentoTotal);
        venta.setTotal(totalFinal);

        // 5. Validar Pagos
        BigDecimal totalPagado = BigDecimal.ZERO;
        List<PagoVenta> pagosVenta = new ArrayList<>();
        
        for (PagoRequestDTO pagoReq : request.getPagos()) {
            PagoVenta pago = new PagoVenta();
            pago.setVenta(venta);
            pago.setMetodoPago(pagoReq.getMetodoPago());
            pago.setMonto(pagoReq.getMonto());
            pago.setFechaPago(LocalDateTime.now());
            pago.setEstado(EstadoRegistro.ACTIVO.getValor());
            pagosVenta.add(pago);
            totalPagado = totalPagado.add(pagoReq.getMonto());
        }

        if (totalPagado.compareTo(totalFinal) != 0) {
            throw new BusinessException("El monto pagado (" + totalPagado + ") no coincide con el total de la venta (" + totalFinal + ")");
        }
        
        venta.setPagos(pagosVenta);

        // 6. Guardar Venta (El Cascade guardará detalles y pagos)
        Venta ventaGuardada = ventaRepository.save(venta);

        return mapToResponse(ventaGuardada, subtotal);
    }

    // ============================================================
    // LÓGICA DE DESCUENTOS
    // ============================================================
    private BigDecimal calcularDescuentos(List<DetalleVentaRequestDTO> detallesReq, BigDecimal subtotal) {
        List<Descuento> descuentos = descuentoRepository.findDescuentosVigentes(LocalDate.now());
        BigDecimal descuentoTotal = BigDecimal.ZERO;

        for (Descuento d : descuentos) {
            // 1. Validar si cumple la condición de monto o cantidad
            boolean cumpleCondicion = false;
            if (d.getTipoCondicion() == TipoCondicionDescuento.MONTO_MINIMO) {
                if (subtotal.compareTo(d.getValorCondicion()) >= 0) cumpleCondicion = true;
            } else if (d.getTipoCondicion() == TipoCondicionDescuento.CANTIDAD_MINIMA) {
                // Sumar cantidad de unidades base de ese producto en el carrito
                BigDecimal cantidadTotalProducto = BigDecimal.ZERO;
                for (DetalleVentaRequestDTO item : detallesReq) {
                    Presentacion p = presentacionRepository.findById(item.getIdPresentacion()).orElse(null);
                    if (p != null && p.getProducto().getId().equals(d.getProducto().getId())) {
                        cantidadTotalProducto = cantidadTotalProducto.add(item.getCantidad().multiply(BigDecimal.valueOf(p.getFactorConversion())));
                    }
                }
                if (cantidadTotalProducto.compareTo(d.getValorCondicion()) >= 0) cumpleCondicion = true;
            }

            // 2. Si cumple, verificar si el producto objetivo está en el carrito
            if (cumpleCondicion) {
                for (DetalleVentaRequestDTO item : detallesReq) {
                    Presentacion p = presentacionRepository.findById(item.getIdPresentacion()).orElse(null);
                    if (p != null && p.getProducto().getId().equals(d.getProducto().getId())) {
                        // Calcular descuento sobre el subtotal de ese item
                        BigDecimal subtotalItem = p.getPrecioVenta().multiply(item.getCantidad());
                        BigDecimal descuentoAplicable = subtotalItem.multiply(d.getPorcentaje()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        descuentoTotal = descuentoTotal.add(descuentoAplicable);
                    }
                }
            }
        }
        return descuentoTotal;
    }

    // ============================================================
    // HISTORIAL DE COMPRAS DE UN CLIENTE
    // ============================================================
    public List<HistorialCompraDTO> obtenerHistorialCompras(Integer idCliente) {
        // 1. Validar que el cliente exista
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente);
        }

        // 2. Buscar sus ventas y mapearlas al DTO
        List<Venta> ventas = ventaRepository.findByClienteId(idCliente);

        return ventas.stream().map(v -> new HistorialCompraDTO(
                v.getId(),
                v.getNumeroNotaVenta(),
                v.getFechaVenta(),
                v.getTotal(),
                v.getEstado()
        )).collect(Collectors.toList());
    }

    // ============================================================
    // AUXILIARES
    // ============================================================
    private String generarCorrelativo() {
        Optional<Venta> ultimaVenta = ventaRepository.findUltimaVenta();
        if (ultimaVenta.isPresent()) {
            String ultimoNumero = ultimaVenta.get().getNumeroNotaVenta();
            // Asumiendo formato NV-000001
            int numero = Integer.parseInt(ultimoNumero.substring(3)) + 1;
            return String.format("NV-%06d", numero);
        }
        return "NV-000001";
    }

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Usuario no autenticado");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private VentaResponseDTO mapToResponse(Venta venta, BigDecimal subtotal) {
        VentaResponseDTO dto = new VentaResponseDTO();
        dto.setId(venta.getId());
        dto.setNumeroNotaVenta(venta.getNumeroNotaVenta());
        dto.setFechaVenta(venta.getFechaVenta());
        dto.setClienteNombre(venta.getCliente() != null ? venta.getCliente().getNombres() : "Cliente Varios");
        dto.setCajeroNombre(venta.getUsuario().getNombres());
        dto.setSubtotal(subtotal);
        dto.setDescuentoTotal(venta.getDescuentoTotal());
        dto.setTotal(venta.getTotal());

        List<VentaResponseDTO.DetalleVentaInfoDTO> detallesDto = venta.getDetalles().stream().map(d -> {
            VentaResponseDTO.DetalleVentaInfoDTO det = new VentaResponseDTO.DetalleVentaInfoDTO();
            det.setProductoNombre(d.getPresentacion().getProducto().getNombre());
            det.setPresentacionTipo(d.getPresentacion().getTipo().name());
            // Para el ticket mostramos las unidades base
            det.setCantidad(d.getCantidad()); 
            det.setPrecioUnitario(d.getPrecioUnitario());
            det.setSubtotal(d.getCantidad().multiply(d.getPrecioUnitario()));
            det.setNumeroLote(d.getLote().getNumeroLote());
            return det;
        }).toList();
        dto.setDetalles(detallesDto);

        List<VentaResponseDTO.PagoInfoDTO> pagosDto = venta.getPagos().stream().map(p -> {
            VentaResponseDTO.PagoInfoDTO pag = new VentaResponseDTO.PagoInfoDTO();
            pag.setMetodoPago(p.getMetodoPago());
            pag.setMonto(p.getMonto());
            return pag;
        }).toList();
        dto.setPagos(pagosDto);

        return dto;
    }
}


