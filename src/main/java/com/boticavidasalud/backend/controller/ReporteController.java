package com.boticavidasalud.backend.controller;

import com.boticavidasalud.backend.dto.ClienteRecurrenteDTO;
import com.boticavidasalud.backend.dto.ProductoMasVendidoDTO;
import com.boticavidasalud.backend.dto.ResumenVentasDTO;
import com.boticavidasalud.backend.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas/resumen")
    public ResponseEntity<ResumenVentasDTO> obtenerResumenVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reporteService.obtenerResumenVentas(inicio, fin));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas/productos-mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoDTO>> obtenerProductosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reporteService.obtenerProductosMasVendidos(inicio, fin));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas/clientes-recurrentes")
    public ResponseEntity<List<ClienteRecurrenteDTO>> obtenerClientesRecurrentes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(reporteService.obtenerClientesRecurrentes(inicio, fin));
    }
}


