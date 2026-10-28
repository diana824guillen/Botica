export interface Lote {
  id: number;
  idProducto: number;
  nombreProducto: string;
  numeroLote: string;
  fechaVencimiento: string;
  cantidad: number;
  estado: number;
  fechaIngreso: string | null;
}

export interface DetalleIngreso {
  idPresentacion: number;
  cantidadPresentacion: number;
}

export interface LoteRequest {
  numeroLote: string;
  fechaVencimiento: string;
  detallesIngreso: DetalleIngreso[];
}

export type MotivoAjuste =
  | 'PRODUCTO_VENCIDO'
  | 'PRODUCTO_DAÑADO'
  | 'PERDIDA_ROBO'
  | 'DEVOLUCION_PROVEEDOR'
  | 'USO_INTERNO_DONACION'
  | 'ERROR_CONTEO_FISICO'
  | 'ERROR_DIGITACION'
  | 'AJUSTE_EMPAQUE';

export interface LoteAjusteRequest {
  idPresentacion: number;
  cantidadAjuste: number;
  motivo: MotivoAjuste;
  observacion: string | null;
}


