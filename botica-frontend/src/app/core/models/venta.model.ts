export type MetodoPago =
  | 'EFECTIVO'
  | 'YAPE'
  | 'PLIN'
  | 'TARJETA'
  | 'TRANSFERENCIA';

// Detalle enviado al backend.
export interface DetalleVentaRequest {
  idPresentacion: number;
  cantidad: number;
}

// Pago enviado al backend.
export interface PagoVentaRequest {
  metodoPago: MetodoPago;
  monto: number;
}

// Datos para previsualizar la venta.
export interface VentaPrevisualizacionRequest {
  detalles: DetalleVentaRequest[];
}

// Resultado de cada detalle de la previsualización.
export interface DetalleVentaPrevisualizacion {
  idProducto: number;
  idPresentacion: number;
  productoNombre: string;
  presentacionTipo: string;
  factorConversion: number;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  stockDisponible: number;
  stockSuficiente: boolean;
}

// Promoción aplicada durante la previsualización.
export interface DescuentoVentaPrevisualizacion {
  idDescuento: number;
  nombre: string;
  productoNombre: string;
  tipoCondicion: string;
  valorCondicion: number;
  porcentaje: number;
  montoDescuento: number;
}

// Respuesta de la previsualización.
export interface VentaPrevisualizacionResponse {
  subtotal: number;
  descuentoTotal: number;
  total: number;
  stockSuficiente: boolean;
  detalles: DetalleVentaPrevisualizacion[];
  descuentosAplicados: DescuentoVentaPrevisualizacion[];
}

// Solicitud para registrar la venta.
export interface VentaRequest {
  idCliente: number | null;
  observacion?: string | null;
  detalles: DetalleVentaRequest[];
  pagos: PagoVentaRequest[];
}

// Detalle devuelto al registrar una venta.
export interface DetalleVentaResponse {
  productoNombre: string;
  presentacionTipo: string;
  cantidad: number;
  cantidadBase: number;
  factorConversion: number;
  precioUnitario: number;
  subtotal: number;
  numeroLote: string;
}

// Pago devuelto por el backend.
export interface PagoVentaResponse {
  metodoPago: MetodoPago;
  monto: number;
}

// Respuesta del registro definitivo.
export interface VentaResponse {
  id: number;
  numeroNotaVenta: string;
  fechaVenta: string;
  clienteNombre: string;
  cajeroNombre: string;
  subtotal: number;
  descuentoTotal: number;
  total: number;
  detalles: DetalleVentaResponse[];
  pagos: PagoVentaResponse[];
}


