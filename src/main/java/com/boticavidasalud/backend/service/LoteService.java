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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoteService {

    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PresentacionRepository presentacionRepository; 

    public LoteService(LoteRepository loteRepository, ProductoRepository productoRepository,
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
    // CREAR LOTE (Calculando stock por presentación física)
    // ============================================================
    public LoteResponseDTO crearLote(Integer idProducto, LoteRequestDTO request) {
        Producto producto = productoRepository.findByIdActivo(idProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o eliminado con ID: " + idProducto));

        loteRepository.findByProductoAndNumeroLote(idProducto, request.getNumeroLote()).ifPresent(l -> {
            throw new BusinessException("Ya existe un lote con ese número para este producto");
        });

        // 1. Calcular el total en unidad base y validar presentaciones
        BigDecimal cantidadTotalBase = BigDecimal.ZERO;
        
        for (DetalleIngresoDTO detalle : request.getDetallesIngreso()) {
            Presentacion p = presentacionRepository.findById(detalle.getIdPresentacion())
                    .orElseThrow(() -> new ResourceNotFoundException("Presentación no encontrada ID: " + detalle.getIdPresentacion()));
            
            if (!p.getProducto().getId().equals(idProducto)) {
                throw new BusinessException("La presentación ID " + p.getId() + " no pertenece al producto seleccionado.");
            }

            BigDecimal factor = BigDecimal.valueOf(p.getFactorConversion());
            cantidadTotalBase = cantidadTotalBase.add(detalle.getCantidadPresentacion().multiply(factor));
        }

        // 2. Crear y guardar el Lote con la cantidad total en unidad base
        Lote lote = new Lote();
        lote.setProducto(producto);
        lote.setNumeroLote(request.getNumeroLote());
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setCantidad(cantidadTotalBase);
        lote.setEstado(EstadoRegistro.ACTIVO.getValor());

        Lote guardado = loteRepository.save(lote);

        // 3. Registrar los movimientos de INGRESO (uno por cada presentación física)
        for (DetalleIngresoDTO detalle : request.getDetallesIngreso()) {
            Presentacion p = presentacionRepository.findById(detalle.getIdPresentacion()).get();
            BigDecimal factor = BigDecimal.valueOf(p.getFactorConversion());
            BigDecimal cantidadBase = detalle.getCantidadPresentacion().multiply(factor);

            registrarMovimiento(
                    guardado, 
                    p.getId(), 
                    detalle.getCantidadPresentacion(), 
                    TipoMovimiento.INGRESO,
                    BigDecimal.ZERO, 
                    cantidadBase, 
                    cantidadBase, 
                    "Ingreso inicial de lote en " + p.getTipo()
            );
        }

        return mapToResponse(guardado);
    }

    // ============================================================
    // AJUSTAR STOCK DE UN LOTE (Merma o Ajuste)
    // ============================================================
    public LoteResponseDTO ajustarLote(Integer id, LoteAjusteDTO request) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));

        if (lote.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede ajustar un lote eliminado");
        }

        Presentacion p = presentacionRepository.findById(request.getIdPresentacion())
                .orElseThrow(() -> new ResourceNotFoundException("Presentación no encontrada"));
                
        if (!p.getProducto().getId().equals(lote.getProducto().getId())) {
            throw new BusinessException("La presentación no pertenece al producto de este lote");
        }

        BigDecimal factor = BigDecimal.valueOf(p.getFactorConversion());
        BigDecimal cantidadBaseAjuste = request.getCantidadAjuste().multiply(factor);

        BigDecimal stockAnterior = lote.getCantidad();
        BigDecimal stockNuevo = stockAnterior.add(cantidadBaseAjuste);

        if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El ajuste dejaría el stock en negativo.");
        }

        lote.setCantidad(stockNuevo);
        Lote actualizado = loteRepository.save(lote);

        // El sistema lee el motivo y sabe automáticamente si es MERMA o AJUSTE
        TipoMovimiento tipoMov = request.getMotivo().getTipoMovimiento();
        
        // Armamos un mensaje de motivo legible para el historial
        String motivoFinal = request.getMotivo().getDescripcion();
        if (request.getObservacion() != null && !request.getObservacion().isEmpty()) {
            motivoFinal += " - " + request.getObservacion();
        }

        registrarMovimiento(
                actualizado,
                p.getId(),
                request.getCantidadAjuste(),
                tipoMov, 
                stockAnterior,
                cantidadBaseAjuste,
                stockNuevo,
                motivoFinal
        );

        return mapToResponse(actualizado);
    }

    // ============================================================
    // CAMBIAR ESTADO Y ELIMINACIÓN
    // ============================================================
    public LoteResponseDTO cambiarEstado(Integer id, Short nuevoEstado) {
        if (nuevoEstado == null || (nuevoEstado != 0 && nuevoEstado != 1)) {
            throw new BusinessException("Estado inválido. Debe ser 0 (Inactivo) o 1 (Activo)");
        }
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
        if (lote.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("No se puede cambiar el estado de un lote eliminado");
        }
        lote.setEstado(nuevoEstado);
        if (nuevoEstado.equals(EstadoRegistro.INACTIVO.getValor())) {
            lote.setFechaDesactivacion(LocalDateTime.now());
        } else {
            lote.setFechaDesactivacion(null);
        }
        return mapToResponse(loteRepository.save(lote));
    }

    public void eliminarLogicamente(Integer id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id));
        if (lote.getEstado().equals(EstadoRegistro.ELIMINADO.getValor())) {
            throw new BusinessException("El lote ya está eliminado");
        }
        BigDecimal cantidadAnterior = lote.getCantidad();
        lote.setEstado(EstadoRegistro.ELIMINADO.getValor());
        lote.setFechaEliminacion(LocalDateTime.now());
        lote.setCantidad(BigDecimal.ZERO);
        loteRepository.save(lote);

        if (cantidadAnterior.compareTo(BigDecimal.ZERO) > 0) {
            registrarMovimiento(
                    lote, null, null, TipoMovimiento.MERMA,
                    cantidadAnterior, cantidadAnterior.negate(), BigDecimal.ZERO,
                    "Eliminación lógica de lote"
            );
        }
    }

    // ============================================================
    // CONSULTAS Y AUXILIARES
    // ============================================================
    public List<LoteResponseDTO> listarLotesPorProducto(Integer idProducto) {
        return loteRepository.listarPorProducto(idProducto).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public LoteResponseDTO obtenerLotePorId(Integer id) {
        return mapToResponse(loteRepository.findByIdActivo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + id)));
    }

    public List<LoteResponseDTO> listarLotesPorVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        return loteRepository.listarPorVencer(hoy, hoy.plusDays(dias)).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<LoteResponseDTO> listarLotesVencidos() {
        return loteRepository.listarVencidos(LocalDate.now()).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void registrarMovimiento(Lote lote, Integer idPresentacion, BigDecimal cantidadPres,
                                     TipoMovimiento tipo, BigDecimal stockAnterior,
                                     BigDecimal cantidad, BigDecimal stockNuevo, String motivo) {
        Usuario usuarioActual = getUsuarioAutenticado();
        Presentacion presentacion = null;
        
        if (idPresentacion != null) {
            presentacion = presentacionRepository.findById(idPresentacion).orElse(null);
        }

        MovimientoInventario mov = new MovimientoInventario();
        mov.setLote(lote);
        mov.setPresentacion(presentacion);
        mov.setUsuario(usuarioActual);
        mov.setTipoMovimiento(tipo);
        mov.setStockAnterior(stockAnterior);
        mov.setCantidad(cantidad);
        mov.setCantidadPresentacion(cantidadPres);
        mov.setStockNuevo(stockNuevo);
        mov.setMotivo(motivo);
        mov.setFecha(LocalDateTime.now());

        movimientoRepository.save(mov);
    }

    private Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Usuario no autenticado");
        }
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado en BD"));
    }

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
}


