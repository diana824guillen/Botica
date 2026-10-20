package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.ClienteRecurrenteDTO;
import com.boticavidasalud.backend.dto.ProductoMasVendidoDTO;
import com.boticavidasalud.backend.dto.ResumenVentasDTO;
import com.boticavidasalud.backend.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final VentaRepository ventaRepository;

    public ReporteService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    // 1. Resumen de Ventas
    public ResumenVentasDTO obtenerResumenVentas(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = inicio.atStartOfDay();
        LocalDateTime fechaFin = fin.atTime(LocalTime.MAX);

        List<Object[]> resultados = ventaRepository.obtenerResumenVentas(fechaInicio, fechaFin);
        
        if (resultados.isEmpty() || resultados.get(0) == null) {
            return new ResumenVentasDTO(0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Object[] resultado = resultados.get(0);
        
        Long cantidad = ((Number) resultado[0]).longValue();
        BigDecimal total = new BigDecimal(resultado[1].toString());
        BigDecimal descuentos = new BigDecimal(resultado[2].toString());

        return new ResumenVentasDTO(cantidad, total, descuentos);
    }

    // 2. Productos Más Vendidos
    public List<ProductoMasVendidoDTO> obtenerProductosMasVendidos(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = inicio.atStartOfDay();
        LocalDateTime fechaFin = fin.atTime(LocalTime.MAX);

        List<Object[]> resultados = ventaRepository.obtenerProductosMasVendidos(fechaInicio, fechaFin);

        return resultados.stream().map(r -> new ProductoMasVendidoDTO(
                (String) r[0],
                new BigDecimal(r[1].toString())
        )).collect(Collectors.toList());
    }

    // 3. Clientes Recurrentes
    public List<ClienteRecurrenteDTO> obtenerClientesRecurrentes(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = inicio.atStartOfDay();
        LocalDateTime fechaFin = fin.atTime(LocalTime.MAX);

        List<Object[]> resultados = ventaRepository.obtenerClientesRecurrentes(fechaInicio, fechaFin);

        return resultados.stream().map(r -> new ClienteRecurrenteDTO(
                r[0] + " " + (r[1] != null ? r[1] : ""),
                ((Number) r[2]).longValue(),
                new BigDecimal(r[3].toString())
        )).collect(Collectors.toList());
    }
}


