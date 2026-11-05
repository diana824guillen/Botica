export type MetodoPago =
  | 'EFECTIVO'
  | 'YAPE'
  | 'PLIN'
  | 'TARJETA'
  | 'TRANSFERENCIA';

export type TipoDocumentoCliente =
  | 'DNI'
  | 'CE'
  | 'PASAPORTE';

export type OrigenDatosCliente =
  | 'RENIEC'
  | 'MANUAL';

// Datos de un producto enviado al backend.
export interface DetalleVentaRequest {
  idPresentacion: number;
  cantidad: number;
}

// Datos de un pago enviado al backend.
export interface PagoVentaRequest {
  metodoPago: MetodoPago;
  monto: number;
  montoRecibido?: number | null;
}

// Datos de un cliente nuevo que se registrará junto con la venta.
export interface ClienteVentaRequest {
  tipoDocumento: TipoDocumentoCliente;
  numeroDocumento: string;
  nombres?: string;
  apellidos?: string;
  correo?: string | null;
  origenDatos: OrigenDatosCliente;
}

// Datos para previsualizar una venta.
export interface VentaPrevisualizacionRequest {
  detalles: DetalleVentaRequest[];
}

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

export interface DescuentoVentaPrevisualizacion {
  idDescuento: number;
  nombre: string;
  productoNombre: string;
  tipoCondicion: string;
  valorCondicion: number;
  porcentaje: number;
  montoDescuento: number;
}

export interface VentaPrevisualizacionResponse {
  subtotal: number;
  descuentoTotal: number;
  total: number;
  stockSuficiente: boolean;
  detalles: DetalleVentaPrevisualizacion[];
  descuentosAplicados: DescuentoVentaPrevisualizacion[];
}

// Solicitud para registrar definitivamente la venta.
export interface VentaRequest {
  idCliente: number | null;
  nuevoCliente?: ClienteVentaRequest | null;
  observacion?: string | null;
  detalles: DetalleVentaRequest[];
  pagos: PagoVentaRequest[];
  totalEsperado: number;
}

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

export interface PagoVentaResponse {
  metodoPago: MetodoPago;
  monto: number;
  montoRecibido: number | null;
  vuelto: number;
}

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


