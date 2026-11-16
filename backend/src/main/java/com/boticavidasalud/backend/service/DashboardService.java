package com.boticavidasalud.backend.service;

import com.boticavidasalud.backend.dto.DashboardMetodoPagoDTO;
import com.boticavidasalud.backend.dto.DashboardProductoFrecuenteDTO;
import com.boticavidasalud.backend.dto.DashboardVentaPeriodoDTO;
import com.boticavidasalud.backend.dto.DashboardVentasResponseDTO;

import com.boticavidasalud.backend.enums.MetodoPago;

import com.boticavidasalud.backend.exception.BusinessException;

import com.boticavidasalud.backend.repository.VentaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal CIEN =
            new BigDecimal("100");

    private final VentaRepository ventaRepository;

    public DashboardService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }


    // ============================================================
    // RESUMEN GENERAL DE VENTAS
    // ============================================================

    public DashboardVentasResponseDTO obtenerResumenVentas(

            LocalDate desde,
            LocalDate hasta,

            LocalDate desdeComparacion,
            LocalDate hastaComparacion) {

        validarRangoFechas(
                desde,
                hasta,
                "El periodo de ventas"
        );

        validarRangoFechas(
                desdeComparacion,
                hastaComparacion,
                "El periodo de comparación"
        );

        // --------------------------------------------------------
        // CONVERTIR FECHAS DEL PERIODO ACTUAL
        // --------------------------------------------------------

        LocalDateTime fechaDesde =
                desde.atStartOfDay();

        // Se incluye todo el día de la fecha final.
        LocalDateTime fechaHasta =
                hasta.plusDays(1).atStartOfDay();


        // --------------------------------------------------------
        // CONVERTIR FECHAS DEL PERIODO DE COMPARACIÓN
        // --------------------------------------------------------

        LocalDateTime fechaDesdeComparacion =
                desdeComparacion.atStartOfDay();

        LocalDateTime fechaHastaComparacion =
                hastaComparacion.plusDays(1).atStartOfDay();


        // --------------------------------------------------------
        // TOTALES DEL PERIODO ACTUAL
        // --------------------------------------------------------

        List<Object[]> resumenActual =
                ventaRepository.obtenerResumenVentasDashboard(
                        fechaDesde,
                        fechaHasta
                );

        Object[] filaResumenActual =
                obtenerFilaResumen(resumenActual);

        BigDecimal totalVendido =
                convertirBigDecimal(filaResumenActual[0]);

        Long cantidadNotas =
                convertirLong(filaResumenActual[1]);


        // --------------------------------------------------------
        // TOTALES DEL PERIODO DE COMPARACIÓN
        // --------------------------------------------------------

        List<Object[]> resumenAnterior =
                ventaRepository.obtenerResumenVentasDashboard(
                        fechaDesdeComparacion,
                        fechaHastaComparacion
                );

        Object[] filaResumenAnterior =
                obtenerFilaResumen(resumenAnterior);

        BigDecimal totalComparacion =
                convertirBigDecimal(filaResumenAnterior[0]);

        Long notasComparacion =
                convertirLong(filaResumenAnterior[1]);


        // --------------------------------------------------------
        // VARIACIÓN PORCENTUAL
        // --------------------------------------------------------

        BigDecimal variacion =
                calcularVariacion(
                        totalVendido,
                        totalComparacion
                );


        // --------------------------------------------------------
        // EVOLUCIÓN DE VENTAS
        // --------------------------------------------------------

        List<DashboardVentaPeriodoDTO> ventasPorPeriodo =
                obtenerVentasPorPeriodo(
                        fechaDesde,
                        fechaHasta,
                        desde,
                        hasta
                );


        // --------------------------------------------------------
        // DISTRIBUCIÓN POR MÉTODO DE PAGO
        // --------------------------------------------------------

        List<DashboardMetodoPagoDTO> metodosPago =
                obtenerMetodosPago(
                        fechaDesde,
                        fechaHasta,
                        totalVendido
                );


        // --------------------------------------------------------
        // PRODUCTOS MÁS SOLICITADOS
        // --------------------------------------------------------

        List<DashboardProductoFrecuenteDTO> productos =
                obtenerProductosMasSolicitados(
                        fechaDesde,
                        fechaHasta
                );


        // --------------------------------------------------------
        // CONSTRUIR RESPUESTA
        // --------------------------------------------------------

        DashboardVentasResponseDTO response =
                new DashboardVentasResponseDTO();

        response.setTotalVendido(totalVendido);
        response.setCantidadNotas(cantidadNotas);

        response.setTotalPeriodoComparacion(totalComparacion);
        response.setCantidadNotasComparacion(notasComparacion);

        response.setVariacionPorcentaje(variacion);

        response.setVentasPorPeriodo(ventasPorPeriodo);
        response.setMetodosPago(metodosPago);
        response.setProductosMasSolicitados(productos);

        long diasPeriodo =
                ChronoUnit.DAYS.between(desde, hasta) + 1;

        response.setAgrupacion(
                diasPeriodo <= 31 ? "DIA" : "MES"
        );

        return response;
    }


    // ============================================================
    // VALIDAR RANGO DE FECHAS
    // ============================================================

    private void validarRangoFechas(
            LocalDate desde,
            LocalDate hasta,
            String nombrePeriodo) {

        if (desde == null || hasta == null) {
            throw new BusinessException(
                    nombrePeriodo + ": las fechas son obligatorias"
            );
        }

        if (desde.isAfter(hasta)) {
            throw new BusinessException(
                    nombrePeriodo
                            + ": la fecha inicial no puede ser posterior "
                            + "a la fecha final"
            );
        }
    }


    // ============================================================
    // CALCULAR VARIACIÓN PORCENTUAL
    // ============================================================

    private BigDecimal calcularVariacion(
            BigDecimal actual,
            BigDecimal anterior) {

        // No existe una comparación porcentual válida
        // cuando el periodo anterior no tuvo ventas.
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return actual
                .subtract(anterior)
                .multiply(CIEN)
                .divide(
                        anterior,
                        2,
                        RoundingMode.HALF_UP
                );
    }


    // ============================================================
    // EVOLUCIÓN DE VENTAS
    // ============================================================

    private List<DashboardVentaPeriodoDTO> obtenerVentasPorPeriodo(

            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,

            LocalDate desde,
            LocalDate hasta) {

        List<Object[]> registros =
                ventaRepository.obtenerVentasAgrupadasPorDia(
                        fechaDesde,
                        fechaHasta
                );

        long dias =
                ChronoUnit.DAYS.between(desde, hasta) + 1;

        boolean agrupacionDiaria = dias <= 31;

        Map<String, DatosPeriodo> acumulados =
                new HashMap<>();


        // --------------------------------------------------------
        // AGRUPAR LOS RESULTADOS RECIBIDOS
        // --------------------------------------------------------

        for (Object[] fila : registros) {

            int anio =
                    ((Number) fila[0]).intValue();

            int mes =
                    ((Number) fila[1]).intValue();

            int dia =
                    ((Number) fila[2]).intValue();

            LocalDate fecha =
                    LocalDate.of(anio, mes, dia);

            String clave = agrupacionDiaria
                    ? fecha.toString()
                    : YearMonth.from(fecha).toString();

            DatosPeriodo datos =
                    acumulados.computeIfAbsent(
                            clave,
                            k -> new DatosPeriodo()
                    );

            datos.totalVendido =
                    datos.totalVendido.add(
                            convertirBigDecimal(fila[3])
                    );

            datos.cantidadNotas +=
                    convertirLong(fila[4]);
        }


        // --------------------------------------------------------
        // GENERAR TODOS LOS PERIODOS
        // INCLUIDOS AQUELLOS SIN VENTAS
        // --------------------------------------------------------

        List<DashboardVentaPeriodoDTO> resultado =
                new ArrayList<>();

        if (agrupacionDiaria) {

            LocalDate fecha = desde;

            while (!fecha.isAfter(hasta)) {

                String clave = fecha.toString();

                resultado.add(
                        crearPeriodoDTO(
                                clave,
                                acumulados.get(clave)
                        )
                );

                fecha = fecha.plusDays(1);
            }

        } else {

            YearMonth mes = YearMonth.from(desde);

            YearMonth ultimoMes = YearMonth.from(hasta);

            while (!mes.isAfter(ultimoMes)) {

                String clave = mes.toString();

                resultado.add(
                        crearPeriodoDTO(
                                clave,
                                acumulados.get(clave)
                        )
                );

                mes = mes.plusMonths(1);
            }
        }

        return resultado;
    }


    // ============================================================
    // CREAR ELEMENTO DE LA EVOLUCIÓN
    // ============================================================

    private DashboardVentaPeriodoDTO crearPeriodoDTO(
            String periodo,
            DatosPeriodo datos) {

        BigDecimal total = datos != null
                ? datos.totalVendido
                : BigDecimal.ZERO;

        Long notas = datos != null
                ? datos.cantidadNotas
                : 0L;

        return new DashboardVentaPeriodoDTO(
                periodo,
                total,
                notas
        );
    }


    // ============================================================
    // RESUMEN DE MÉTODOS DE PAGO
    // ============================================================

    private List<DashboardMetodoPagoDTO> obtenerMetodosPago(

            LocalDateTime desde,
            LocalDateTime hasta,

            BigDecimal totalVendido) {

        List<Object[]> registros =
                ventaRepository.obtenerResumenMetodosPago(
                        desde,
                        hasta
                );

        Map<MetodoPago, DatosMetodoPago> acumulados =
                new EnumMap<>(MetodoPago.class);


        // Inicializar todos los métodos de pago.
        // Así también podremos mostrar métodos con importe cero.

        for (MetodoPago metodo : MetodoPago.values()) {
            acumulados.put(
                    metodo,
                    new DatosMetodoPago()
            );
        }


        // Procesar los importes agrupados recibidos del repositorio.

        for (Object[] fila : registros) {

            MetodoPago metodo =
                    (MetodoPago) fila[0];

            DatosMetodoPago datos =
                    acumulados.get(metodo);

            if (datos == null) {
                continue;
            }

            datos.monto =
                    convertirBigDecimal(fila[1]);

            datos.cantidadPagos =
                    convertirLong(fila[2]);
        }


        // Construir la respuesta.

        List<DashboardMetodoPagoDTO> resultado =
                new ArrayList<>();

        for (MetodoPago metodo : MetodoPago.values()) {

            DatosMetodoPago datos =
                    acumulados.get(metodo);

            BigDecimal porcentaje = BigDecimal.ZERO;

            if (totalVendido.compareTo(BigDecimal.ZERO) > 0) {

                porcentaje = datos.monto
                        .multiply(CIEN)
                        .divide(
                                totalVendido,
                                2,
                                RoundingMode.HALF_UP
                        );
            }

            resultado.add(
                    new DashboardMetodoPagoDTO(
                            metodo.name(),
                            datos.monto,
                            datos.cantidadPagos,
                            porcentaje
                    )
            );
        }

        return resultado;
    }


    // ============================================================
    // TOP 10 DE PRODUCTOS MÁS SOLICITADOS
    // ============================================================

    private List<DashboardProductoFrecuenteDTO>
            obtenerProductosMasSolicitados(

                    LocalDateTime desde,
                    LocalDateTime hasta) {

        List<Object[]> registros =
                ventaRepository.obtenerProductosMasSolicitadosDashboard(
                        desde,
                        hasta,
                        PageRequest.of(0, 10)
                );

        List<DashboardProductoFrecuenteDTO> resultado =
                new ArrayList<>();

        for (Object[] fila : registros) {

            Integer idProducto =
                    ((Number) fila[0]).intValue();

            String nombre =
                    fila[1] != null
                            ? fila[1].toString()
                            : "Producto sin nombre";

            Long cantidadNotas =
                    convertirLong(fila[2]);

            resultado.add(
                    new DashboardProductoFrecuenteDTO(
                            idProducto,
                            nombre,
                            cantidadNotas
                    )
            );
        }

        return resultado;
    }


    // ============================================================
    // OBTENER FILA DEL RESUMEN DE VENTAS
    // ============================================================

    private Object[] obtenerFilaResumen(List<Object[]> resultados) {

        // Si no existen resultados, devolver valores por defecto.
        if (resultados == null || resultados.isEmpty()) {
                return new Object[]{null, 0L};
        }

        Object[] fila = resultados.get(0);

        // Comprobar que la consulta devuelva ambos valores.
        if (fila == null || fila.length < 2) {
                throw new IllegalStateException(
                        "La consulta del resumen de ventas no devolvió "
                        + "el total y la cantidad de notas esperados."
                );
        }

        return fila;
    }

    // ============================================================
    // CONVERSIONES DE VALORES
    // ============================================================

    private BigDecimal convertirBigDecimal(Object valor) {

        if (valor == null) {
            return BigDecimal.ZERO;
        }

        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }

        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }

        return BigDecimal.ZERO;
    }


    private Long convertirLong(Object valor) {

        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        return 0L;
    }


    // ============================================================
    // CLASES AUXILIARES
    // ============================================================

    private static class DatosPeriodo {

        private BigDecimal totalVendido =
                BigDecimal.ZERO;

        private long cantidadNotas = 0;
    }


    private static class DatosMetodoPago {

        private BigDecimal monto =
                BigDecimal.ZERO;

        private long cantidadPagos = 0;
    }

}


