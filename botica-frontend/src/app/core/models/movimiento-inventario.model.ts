export type TipoMovimientoInventario =
  | 'INGRESO'
  | 'SALIDA_VENTA'
  | 'AJUSTE'
  | 'MERMA'
  | 'DEVOLUCION';

export interface MovimientoInventario {
  id: number;
  fecha: string;
  tipoMovimiento: TipoMovimientoInventario;

  idProducto: number | null;
  productoNombre: string | null;

  idPresentacion: number | null;
  tipoPresentacion: string | null;
  factorConversion: number | null;

  idLote: number | null;
  numeroLote: string | null;

  idUsuario: number | null;
  usuarioUsername: string | null;
  usuarioNombre: string | null;

  stockAnterior: number;
  cantidad: number;
  stockNuevo: number;
  cantidadPresentacion: number | null;

  motivo: string | null;
}

export interface PaginaMovimientoResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}


