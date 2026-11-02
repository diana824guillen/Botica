export type TipoCondicionDescuento =
  | 'MONTO_MINIMO'
  | 'CANTIDAD_MINIMA';

export interface Descuento {
  id: number;
  productoNombre: string;
  nombre: string;
  tipoCondicion: TipoCondicionDescuento;
  valorCondicion: number;
  porcentaje: number;
  fechaInicio: string | null;
  fechaFin: string | null;
  estado: number;
}

export interface DescuentoRequest {
  idProducto: number;
  nombre: string;
  tipoCondicion: TipoCondicionDescuento;
  valorCondicion: number;
  porcentaje: number;
  fechaInicio: string | null;
  fechaFin: string | null;
}

export interface DescuentoUpdateRequest {
  nombre: string;
  tipoCondicion: TipoCondicionDescuento;
  valorCondicion: number;
  porcentaje: number;
  fechaInicio: string | null;
  fechaFin: string | null;
}


