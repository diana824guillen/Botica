import type { MetodoPago } from './venta.model';
import type { TipoPresentacion } from './producto.model';

// ============================================================
// RESUMEN DE VENTAS
// ============================================================

export interface DashboardVentasResponse {
  totalVendido: number;
  cantidadNotas: number;

  totalPeriodoComparacion: number;
  cantidadNotasComparacion: number;

  // Puede ser null cuando el periodo anterior no tuvo ventas.
  variacionPorcentaje: number | null;

  agrupacion: 'DIA' | 'MES';

  ventasPorPeriodo: DashboardVentaPeriodo[];
  metodosPago: DashboardMetodoPago[];
  productosMasSolicitados: DashboardProductoFrecuente[];
}

export interface DashboardVentaPeriodo {
  // Formato YYYY-MM-DD o YYYY-MM.
  periodo: string;
  totalVendido: number;
  cantidadNotas: number;
}

export interface DashboardMetodoPago {
  metodoPago: MetodoPago;
  monto: number;
  cantidadPagos: number;
  porcentaje: number;
}

export interface DashboardProductoFrecuente {
  idProducto: number;
  nombreProducto: string;
  cantidadNotas: number;
}

// ============================================================
// RESUMEN DE INVENTARIO
// ============================================================

export interface DashboardInventarioResponse {
  diasAnticipacion: number;

  cantidadPresentacionesAgotadas: number;
  cantidadPresentacionesPorReponer: number;

  cantidadLotesVencidos: number;
  cantidadLotesProximosAVencer: number;

  presentacionesAgotadas: DashboardPresentacionAlerta[];
  presentacionesPorReponer: DashboardPresentacionAlerta[];

  lotesVencidos: DashboardLoteAlerta[];
  lotesProximosAVencer: DashboardLoteAlerta[];
}

export interface DashboardPresentacionAlerta {
  idProducto: number;
  nombreProducto: string;

  idPresentacion: number;
  tipoPresentacion: TipoPresentacion;

  factorConversion: number;

  stockDisponible: number;
  stockMinimo: number;
}

export interface DashboardLoteAlerta {
  idLote: number;

  idProducto: number;
  nombreProducto: string;

  numeroLote: string;

  // Fecha en formato YYYY-MM-DD.
  fechaVencimiento: string;

  // Negativo cuando el lote ya está vencido.
  diasRestantes: number;

  existenciasPresentaciones: DashboardLotePresentacionInfo[];
}

export interface DashboardLotePresentacionInfo {
  idPresentacion: number;
  tipo: TipoPresentacion;
  factorConversion: number;
  cantidad: number;
}


