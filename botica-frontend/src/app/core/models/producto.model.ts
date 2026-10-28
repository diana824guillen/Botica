export type TipoPresentacion =
  | 'UNIDAD'
  | 'BLISTER'
  | 'CAJA'
  | 'FRASCO'
  | 'TIRA'
  | 'OTRO';

export interface Presentacion {
  id: number;
  tipo: TipoPresentacion;
  factorConversion: number;
  precioVenta: number;
  estado: number;
}

export interface PresentacionRequest {
  id?: number;
  tipo: TipoPresentacion;
  factorConversion: number;
  precioVenta: number;
}

export interface Producto {
  id: number;

  categoriaId: number;
  categoriaNombre: string;

  nombre: string;
  descripcion: string | null;

  requiereReceta: boolean;

  stockMinimo: number;
  stockActual: number;

  estado: number;
  fechaCreacion: string;

  presentaciones: Presentacion[];
}

export interface ProductoRequest {
  categoriaId: number;
  nombre: string;
  descripcion: string | null;
  requiereReceta: boolean;
  stockMinimo: number;
  presentaciones: PresentacionRequest[];
}

export interface ProductoUpdateRequest {
  categoriaId: number;
  nombre: string;
  descripcion: string | null;
  requiereReceta: boolean;
  stockMinimo: number;
}


