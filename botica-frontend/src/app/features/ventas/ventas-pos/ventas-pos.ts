import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  Producto,
  Presentacion
} from '../../../core/models/producto.model';

import { Cliente } from '../../../core/models/cliente.model';
import { ClienteService } from '../../clientes/services/cliente';
import { ProductoService } from '../../productos/services/producto';
import { VentaService } from '../services/venta';

import {
  ClienteVentaRequest,
  MetodoPago,
  PagoVentaRequest,
  TipoDocumentoCliente,
  VentaPrevisualizacionRequest,
  VentaPrevisualizacionResponse
} from '../../../core/models/venta.model';

interface ItemCarrito {
  idProducto: number;
  idPresentacion: number;
  productoNombre: string;
  presentacionTipo: string;
  factorConversion: number;
  precioUnitario: number;
  stockDisponible: number;
  cantidad: number;
}

interface CategoriaFiltro {
  id: number;
  nombre: string;
}

type ModoClientePOS = 'NINGUNO' | 'EXISTENTE' | 'NUEVO';
type ModoRegistroNuevo = 'AUTO' | 'MANUAL';

interface PagoFormulario {
  metodoPago: MetodoPago;
  monto: number;
  montoRecibido: number | null;
}

@Component({
  selector: 'app-ventas-pos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './ventas-pos.html',
  styleUrls: ['./ventas-pos.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VentasPosComponent implements OnInit, OnDestroy {

  private productoService = inject(ProductoService);
  private clienteService = inject(ClienteService);
  private ventaService = inject(VentaService);
  private cdr = inject(ChangeDetectorRef);

  // ============================================================
  // DATOS REACTIVOS DEL CATALOGO
  // ============================================================

  productos = signal<Producto[]>([]);
  categoriasDisponibles = signal<CategoriaFiltro[]>([]);
  productosFiltrados = signal<Producto[]>([]);
  cargandoProductos = signal(false);
  errorCarga = signal('');

  // ============================================================
  // FILTROS
  // ============================================================

  busqueda = '';
  categoriaSeleccionada: number | null = null;
  mostrarSinStock = false;

  // Solo un producto puede estar desplegado a la vez.
  productoExpandidoId: number | null = null;

  // ============================================================
  // CLIENTE DE LA VENTA
  // ============================================================

  modoCliente: ModoClientePOS = 'NINGUNO';
  
  // Controla si la sección opcional de cliente está desplegada.
  mostrarPanelCliente = false;

  // Búsqueda de un cliente existente.
  documentoBusqueda = '';
  buscandoCliente = false;
  clienteSeleccionado: Cliente | null = null;
  mensajeBusquedaCliente = '';
  tipoMensajeBusqueda: 'success' | 'error' | '' = '';

  // Datos de un cliente nuevo.
  nuevoCliente = {
    tipoDocumento: 'DNI' as TipoDocumentoCliente,
    numeroDocumento: '',
    nombres: '',
    apellidos: '',
    correo: ''
  };

  modoRegistroNuevo: ModoRegistroNuevo = 'AUTO';
  errorNuevoCliente = '';

  // ============================================================
  // CARRITO
  // ============================================================

  carrito: ItemCarrito[] = [];

  // ============================================================
  // PREVISUALIZACIÓN DEL SERVIDOR
  // ============================================================

  previsualizacion = signal<VentaPrevisualizacionResponse | null>(null);
  cargandoPrevisualizacion = signal(false);
  errorPrevisualizacion = signal('');

  // ============================================================
  // MÉTODOS DE PAGO
  // ============================================================

  pagos: PagoFormulario[] = [];

  readonly metodosPago: {
    valor: MetodoPago;
    nombre: string;
  }[] = [
    { valor: 'EFECTIVO', nombre: 'Efectivo' },
    { valor: 'YAPE', nombre: 'Yape' },
    { valor: 'PLIN', nombre: 'Plin' },
    { valor: 'TARJETA', nombre: 'Tarjeta' },
    { valor: 'TRANSFERENCIA', nombre: 'Transferencia' }
  ];

  errorPagos = '';

  // ============================================================
  // NOTIFICACIONES
  // ============================================================

  mensajeCarrito = signal('');
  tipoMensaje = signal<'success' | 'warning'>('success');

  private mensajeTimeout: ReturnType<typeof setTimeout> | null = null;

  // ============================================================
  // CICLO DE VIDA
  // ============================================================

  ngOnInit(): void {
    this.cargarProductos();
  }

  ngOnDestroy(): void {
    if (this.mensajeTimeout) {
      clearTimeout(this.mensajeTimeout);
      this.mensajeTimeout = null;
    }
  }

  // ============================================================
  // CARGAR PRODUCTOS
  // ============================================================

  cargarProductos(): void {

    this.cargandoProductos.set(true);
    this.errorCarga.set('');

    this.productoService
      .listarProductos()
      .pipe(
        finalize(() => {
          this.cargandoProductos.set(false);
        })
      )
      .subscribe({

        next: respuesta => {

          const productosActivos = (respuesta || [])
            .filter(producto => producto.estado === 1)
            .map(producto => ({
              ...producto,
              presentaciones: (producto.presentaciones || [])
                .filter(presentacion => presentacion.estado === 1)
            }));

          this.productos.set(productosActivos);

          this.reconstruirCategorias();
          this.actualizarProductosFiltrados();

          if (
            this.productoExpandidoId !== null &&
            !productosActivos.some(
              producto => producto.id === this.productoExpandidoId
            )
          ) {
            this.productoExpandidoId = null;
          }
        },

        error: err => {

          console.error('Error al cargar el catálogo:', err);

          if (err?.status === 403) {

            this.errorCarga.set(
              'Tu perfil no tiene permiso para consultar el catálogo. Se necesita el permiso PRODUCTO_LEER.'
            );

          } else if (err?.status === 0) {

            this.errorCarga.set(
              'No se pudo conectar con el servidor.'
            );

          } else {

            this.errorCarga.set(
              err?.error?.mensaje ||
              err?.error?.message ||
              'No se pudo cargar el catálogo de productos.'
            );
          }
        }
      });
  }

  // ============================================================
  // DISPONIBILIDAD DE PRODUCTOS
  // ============================================================

  obtenerStock(presentacion: Presentacion): number {

    const stock = Number(presentacion.stockDisponible ?? 0);

    if (!Number.isFinite(stock)) {
      return 0;
    }

    return Math.max(0, Math.floor(stock));
  }

  tieneStock(producto: Producto): boolean {

    return (producto.presentaciones || []).some(
      presentacion => this.obtenerStock(presentacion) > 0
    );
  }

  obtenerPresentacionesDisponibles(producto: Producto): number {

    return (producto.presentaciones || []).filter(
      presentacion => this.obtenerStock(presentacion) > 0
    ).length;
  }

  // ============================================================
  // RECONSTRUIR CATEGORÍAS
  // ============================================================

  private reconstruirCategorias(): void {

    const categorias = new Map<number, string>();

    for (const producto of this.productos()) {
      categorias.set(
        producto.categoriaId,
        producto.categoriaNombre
      );
    }

    const resultado: CategoriaFiltro[] = Array.from(
      categorias.entries()
    )
      .map(([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));

    this.categoriasDisponibles.set(resultado);
  }

  // ============================================================
  // ACTUALIZAR PRODUCTOS FILTRADOS
  // ============================================================

  actualizarProductosFiltrados(): void {

    const termino = this.busqueda
      .trim()
      .toLocaleLowerCase();

    const resultado = this.productos().filter(producto => {

      const nombre = (producto.nombre || '').toLocaleLowerCase();

      const descripcion = (
        producto.descripcion || ''
      ).toLocaleLowerCase();

      const categoria = (
        producto.categoriaNombre || ''
      ).toLocaleLowerCase();

      const coincideTexto =
        !termino ||
        nombre.includes(termino) ||
        descripcion.includes(termino) ||
        categoria.includes(termino);

      const coincideCategoria =
        this.categoriaSeleccionada === null ||
        producto.categoriaId === this.categoriaSeleccionada;

      const coincideStock =
        this.mostrarSinStock || this.tieneStock(producto);

      return coincideTexto &&
        coincideCategoria &&
        coincideStock;
    });

    this.productosFiltrados.set(resultado);
  }

  // ============================================================
  // CAMBIOS EN LOS FILTROS
  // ============================================================

  cambiarBusqueda(valor: string): void {
    this.busqueda = valor || '';
    this.actualizarProductosFiltrados();
  }

  cambiarCategoria(id: number | null): void {
    this.categoriaSeleccionada = id;
    this.actualizarProductosFiltrados();
  }

  cambiarMostrarSinStock(valor: boolean): void {
    this.mostrarSinStock = valor;
    this.actualizarProductosFiltrados();
  }

  limpiarFiltros(): void {

    this.busqueda = '';
    this.categoriaSeleccionada = null;
    this.mostrarSinStock = false;
    this.productoExpandidoId = null;

    this.actualizarProductosFiltrados();
  }

  // ============================================================
  // IDENTIFICADORES ESTABLES PARA LOS LISTADOS
  // ============================================================

  trackByProductoId(
    index: number,
    producto: Producto
  ): number {
    return producto.id;
  }

  trackByCategoriaId(
    index: number,
    categoria: CategoriaFiltro
  ): number {
    return categoria.id;
  }

  trackByPresentacionId(
    index: number,
    presentacion: Presentacion
  ): number {
    return presentacion.id;
  }

  trackByCarritoId(
    index: number,
    item: ItemCarrito
  ): number {
    return item.idPresentacion;
  }

  // ============================================================
  // DESPLEGAR PRODUCTOS
  // ============================================================

  alternarProducto(idProducto: number): void {

    this.productoExpandidoId =
      this.productoExpandidoId === idProducto
        ? null
        : idProducto;
  }

  // ============================================================
  // FORMATO DE PRESENTACIONES
  // ============================================================

  obtenerNombrePresentacion(tipo: string): string {

    const nombres: Record<string, string> = {
      UNIDAD: 'Unidad',
      BLISTER: 'Blíster',
      CAJA: 'Caja',
      FRASCO: 'Frasco',
      TIRA: 'Tira',
      OTRO: 'Otro'
    };

    return nombres[tipo] || tipo;
  }

  obtenerCantidadEnCarrito(idPresentacion: number): number {

    return this.carrito.find(
      item => item.idPresentacion === idPresentacion
    )?.cantidad ?? 0;
  }

  // ============================================================
  // AGREGAR AL CARRITO
  // ============================================================

  agregarPresentacion(
    producto: Producto,
    presentacion: Presentacion
  ): void {

    const stockDisponible = this.obtenerStock(presentacion);

    const itemExistente = this.carrito.find(
      item => item.idPresentacion === presentacion.id
    );

    const cantidadActual = itemExistente?.cantidad ?? 0;

    if (cantidadActual + 1 > stockDisponible) {

      this.mostrarMensaje(
        'No hay suficientes existencias para agregar otra unidad de esta presentación.',
        'warning'
      );

      return;
    }

    if (itemExistente) {

      itemExistente.cantidad++;

    } else {

      this.carrito.push({
        idProducto: producto.id,
        idPresentacion: presentacion.id,
        productoNombre: producto.nombre,
        presentacionTipo: presentacion.tipo,
        factorConversion: presentacion.factorConversion,
        precioUnitario: Number(presentacion.precioVenta),
        stockDisponible,
        cantidad: 1
      });
    }

    this.invalidarPrevisualizacion();

    this.mostrarMensaje(
      `${producto.nombre} agregado al carrito.`,
      'success'
    );
  }

  // ============================================================
  // MODIFICAR CANTIDADES
  // ============================================================

  aumentarCantidad(idPresentacion: number): void {

    const item = this.carrito.find(
      elemento => elemento.idPresentacion === idPresentacion
    );

    if (!item) {
      return;
    }

    if (item.cantidad + 1 > item.stockDisponible) {

      this.mostrarMensaje(
        'Has alcanzado el stock disponible de esta presentación.',
        'warning'
      );

      return;
    }

    item.cantidad++;

    this.invalidarPrevisualizacion();
    this.limpiarMensaje();
  }

  disminuirCantidad(idPresentacion: number): void {

    const item = this.carrito.find(
      elemento => elemento.idPresentacion === idPresentacion
    );

    if (!item) {
      return;
    }

    if (item.cantidad <= 1) {
      this.quitarProducto(idPresentacion);
      return;
    }

    item.cantidad--;

    this.invalidarPrevisualizacion();
    this.limpiarMensaje();
  }

  quitarProducto(idPresentacion: number): void {

    this.carrito = this.carrito.filter(
      item => item.idPresentacion !== idPresentacion
    );

    this.invalidarPrevisualizacion();
    this.limpiarMensaje();
  }

  limpiarCarrito(): void {

    this.carrito = [];

    this.invalidarPrevisualizacion();
    this.limpiarMensaje();
  }

  // ============================================================
  // TOTALES DEL CARRITO
  // ============================================================

  get cantidadTotalProductos(): number {

    return this.carrito.reduce(
      (total, item) => total + item.cantidad,
      0
    );
  }

  get cantidadTiposProductos(): number {
    return this.carrito.length;
  }

  get subtotalCarrito(): number {

    const subtotal = this.carrito.reduce(
      (total, item) =>
        total + item.precioUnitario * item.cantidad,
      0
    );

    return Number(subtotal.toFixed(2));
  }

  calcularSubtotalItem(item: ItemCarrito): number {

    return Number(
      (item.precioUnitario * item.cantidad).toFixed(2)
    );
  }

  // ============================================================
  // ORGANIZACIÓN DEL PROCESO DE VENTA
  // ============================================================

  // Indica si el carrito ya fue calculado y tiene existencias suficientes.
  get ventaCalculada(): boolean {
    return this.previsualizacion()?.stockSuficiente === true;
  }

  // Resumen breve del cliente seleccionado o del estado actual.
  get resumenCliente(): string {

    if (this.clienteSeleccionado) {
      return `${this.clienteSeleccionado.nombres} ${this.clienteSeleccionado.apellidos}`.trim();
    }

    if (this.modoCliente === 'NUEVO') {

      const documento = this.nuevoCliente.numeroDocumento.trim();

      if (documento) {
        return `Nuevo cliente · ${documento}`;
      }

      return 'Registrar un nuevo cliente';
    }

    if (this.modoCliente === 'EXISTENTE') {
      return 'Buscar cliente registrado';
    }

    return 'Sin cliente asociado';
  }

  // Abre o contrae la sección opcional de cliente.
  alternarPanelCliente(): void {
    this.mostrarPanelCliente = !this.mostrarPanelCliente;
  }

  // ============================================================
  // SELECCIÓN DEL TIPO DE CLIENTE
  // ============================================================

  cambiarModoCliente(modo: ModoClientePOS): void {

    if (this.modoCliente === modo) {

      // Al seleccionar nuevamente "Sin cliente", cierra la sección.
      if (modo === 'NINGUNO') {
        this.mostrarPanelCliente = false;
      }

      return;
    }

    this.limpiarDatosCliente();

    this.modoCliente = modo;

    // Las opciones de cliente abren la sección para mostrar sus campos.
    this.mostrarPanelCliente = modo !== 'NINGUNO';
  }

  // ============================================================
  // BUSCAR CLIENTE EXISTENTE
  // ============================================================

  buscarCliente(): void {
    const documento = this.documentoBusqueda
      .trim()
      .toUpperCase();

    if (!documento) {

      this.mensajeBusquedaCliente =
        'Ingresa el número de documento del cliente.';

      this.tipoMensajeBusqueda = 'error';

      return;
    }

    this.buscandoCliente = true;
    this.clienteSeleccionado = null;
    this.mensajeBusquedaCliente = '';
    this.tipoMensajeBusqueda = '';

    this.clienteService
      .buscarPorDocumento(documento)
      .pipe(
        finalize(() => {

          this.buscandoCliente = false;

          // Fuerza la actualización de la vista con OnPush.
          this.cdr.markForCheck();

        })
      )
      .subscribe({

        next: cliente => {

          this.clienteSeleccionado = cliente;

          this.mensajeBusquedaCliente =
            'Cliente encontrado correctamente.';

          this.tipoMensajeBusqueda = 'success';
        },

        error: err => {

          if (err?.status === 404) {

            this.mensajeBusquedaCliente =
              'No se encontró un cliente con ese documento. Puedes registrarlo como nuevo.';

          } else {

            console.error(
              'Error inesperado al buscar cliente:',
              err
            );

            if (err?.status === 403) {

              this.mensajeBusquedaCliente =
                'Tu perfil no tiene permiso para consultar clientes. Se necesita el permiso CLIENTE_LEER.';

            } else if (err?.status === 0) {

              this.mensajeBusquedaCliente =
                'No se pudo conectar con el servidor.';

            } else {

              this.mensajeBusquedaCliente =
                err?.error?.mensaje ||
                err?.error?.message ||
                'Ocurrió un error al consultar el cliente.';
            }
          }

          this.tipoMensajeBusqueda = 'error';
        }
      });
  }

  quitarClienteSeleccionado(): void {
    this.limpiarDatosCliente();
    this.modoCliente = 'NINGUNO';
  }

  // ============================================================
  // DATOS DE UN CLIENTE NUEVO
  // ============================================================

  cambiarTipoDocumentoNuevo(tipo: TipoDocumentoCliente): void {

    this.nuevoCliente.tipoDocumento = tipo;
    this.nuevoCliente.numeroDocumento = '';

    this.errorNuevoCliente = '';

    // RENIEC solo está disponible para DNI.
    if (tipo !== 'DNI') {
      this.modoRegistroNuevo = 'MANUAL';
    }
  }

  cambiarModoRegistroNuevo(modo: ModoRegistroNuevo): void {

    if (modo === 'AUTO' && this.nuevoCliente.tipoDocumento !== 'DNI') {
      return;
    }

    this.modoRegistroNuevo = modo;
    this.errorNuevoCliente = '';

    if (modo === 'AUTO') {
      this.nuevoCliente.nombres = '';
      this.nuevoCliente.apellidos = '';
    }
  }

  normalizarDocumentoNuevo(event: Event): void {

    const input = event.target as HTMLInputElement;
    const tipo = this.nuevoCliente.tipoDocumento;

    let valor = input.value;

    if (tipo === 'DNI') {

      valor = valor
        .replace(/[^0-9]/g, '')
        .substring(0, 8);

    } else if (tipo === 'CE') {

      valor = valor
        .replace(/[^a-zA-Z0-9]/g, '')
        .substring(0, 11)
        .toUpperCase();

    } else {

      valor = valor
        .replace(/[^a-zA-Z0-9]/g, '')
        .substring(0, 15)
        .toUpperCase();
    }

    if (input.value !== valor) {
      input.value = valor;
    }

    this.nuevoCliente.numeroDocumento = valor;
    this.errorNuevoCliente = '';
  }

  normalizarNombreNuevo(
    event: Event,
    campo: 'nombres' | 'apellidos'
  ): void {

    const input = event.target as HTMLInputElement;

    const valor = input.value.replace(
      /[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g,
      ''
    );

    if (input.value !== valor) {
      input.value = valor;
    }

    this.nuevoCliente[campo] = valor;
    this.errorNuevoCliente = '';
  }

  validarNuevoCliente(): string | null {

    const documento = this.nuevoCliente.numeroDocumento.trim();

    if (this.nuevoCliente.tipoDocumento === 'DNI') {

      if (!/^\d{8}$/.test(documento)) {
        return 'El DNI debe contener exactamente 8 dígitos.';
      }

    } else if (this.nuevoCliente.tipoDocumento === 'CE') {

      if (!/^[a-zA-Z0-9]{9,11}$/.test(documento)) {
        return 'El carné de extranjería debe tener entre 9 y 11 caracteres alfanuméricos.';
      }

    } else {

      if (!/^[a-zA-Z0-9]{9,15}$/.test(documento)) {
        return 'El pasaporte debe tener entre 9 y 15 caracteres alfanuméricos.';
      }
    }

    if (
      this.modoRegistroNuevo === 'AUTO' &&
      this.nuevoCliente.tipoDocumento !== 'DNI'
    ) {
      return 'La consulta automática solo está disponible para DNI.';
    }

    if (this.modoRegistroNuevo === 'MANUAL') {

      if (!this.nuevoCliente.nombres.trim()) {
        return 'Ingresa los nombres del cliente.';
      }

      if (!this.nuevoCliente.apellidos.trim()) {
        return 'Ingresa los apellidos del cliente.';
      }

      const patronNombre = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;

      if (
        !patronNombre.test(this.nuevoCliente.nombres.trim()) ||
        !patronNombre.test(this.nuevoCliente.apellidos.trim())
      ) {
        return 'Los nombres y apellidos solo deben contener letras.';
      }
    }

    const correo = this.nuevoCliente.correo.trim();

    if (
      correo &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)
    ) {
      return 'Ingresa un correo electrónico válido.';
    }

    return null;
  }

  // Construye el objeto que se enviará dentro de VentaRequest.
  // No registra al cliente de manera independiente.
  obtenerNuevoClienteRequest(): ClienteVentaRequest | null {

    const error = this.validarNuevoCliente();

    if (error) {
      this.errorNuevoCliente = error;
      return null;
    }

    return {
      tipoDocumento: this.nuevoCliente.tipoDocumento,
      numeroDocumento: this.nuevoCliente.numeroDocumento.trim(),
      nombres: this.modoRegistroNuevo === 'MANUAL'
        ? this.nuevoCliente.nombres.trim()
        : undefined,
      apellidos: this.modoRegistroNuevo === 'MANUAL'
        ? this.nuevoCliente.apellidos.trim()
        : undefined,
      correo: this.nuevoCliente.correo.trim() || null,
      origenDatos: this.modoRegistroNuevo === 'AUTO'
        ? 'RENIEC'
        : 'MANUAL'
    };
  }

  // ============================================================
  // PREVISUALIZAR VENTA EN EL SERVIDOR
  // ============================================================

  previsualizarVenta(): void {

    if (this.carrito.length === 0) {

      this.mostrarMensaje(
        'Agrega al menos un producto antes de calcular la venta.',
        'warning'
      );

      return;
    }

    const request: VentaPrevisualizacionRequest = {
      detalles: this.carrito.map(item => ({
        idPresentacion: item.idPresentacion,
        cantidad: item.cantidad
      }))
    };

    this.cargandoPrevisualizacion.set(true);
    this.errorPrevisualizacion.set('');
    this.previsualizacion.set(null);

    // Los pagos anteriores ya no corresponden al nuevo cálculo.
    this.pagos = [];
    this.errorPagos = '';

    this.ventaService
      .previsualizarVenta(request)
      .pipe(
        finalize(() => {
          this.cargandoPrevisualizacion.set(false);
        })
      )
      .subscribe({

        next: respuesta => {

          this.previsualizacion.set(respuesta);

          if (respuesta.stockSuficiente) {

            this.inicializarPagos();

            this.mostrarMensaje(
              'La venta se calculó correctamente. Ya puedes continuar con el pago.',
              'success'
            );

          } else {

            this.pagos = [];
            this.errorPagos = '';

            this.mostrarMensaje(
              'Algunos productos no tienen existencias suficientes. Revisa las cantidades.',
              'warning'
            );
          }
        },

        error: err => {

          console.error('Error al calcular la venta:', err);

          this.errorPrevisualizacion.set(
            this.obtenerMensajeError(
              err,
              'No se pudo actualizar el total de la venta. Inténtalo nuevamente.'
            )
          );
        }
      });
  }

  // El cálculo deja de ser válido cuando se modifica el carrito.
  private invalidarPrevisualizacion(): void {
    this.previsualizacion.set(null);
    this.errorPrevisualizacion.set('');
    // El carrito cambió, por lo que los pagos anteriores ya no deben utilizarse.
    this.pagos = [];
    this.errorPagos = '';
  }

  // ============================================================
  // GESTIÓN DE MÉTODOS DE PAGO
  // ============================================================

  // Total de la venta calculado con los precios y descuentos vigentes.
  get totalVenta(): number {
    return Number(this.previsualizacion()?.total ?? 0);
  }

  // Suma de los importes distribuidos entre los métodos de pago.
  get totalPagado(): number {

    return Number(
      this.pagos.reduce(
        (total, pago) => total + (Number(pago.monto) || 0),
        0
      ).toFixed(2)
    );
  }

  // Importe que todavía falta distribuir.
  get saldoPendiente(): number {

    return Number(
      Math.max(0, this.totalVenta - this.totalPagado).toFixed(2)
    );
  }

  // Diferencia cuando los importes superan el total de la venta.
  get excesoPago(): number {

    return Number(
      Math.max(0, this.totalPagado - this.totalVenta).toFixed(2)
    );
  }

  // Comprueba si se puede agregar otro método de pago.
  get puedeAgregarPago(): boolean {

    if (this.pagos.length >= this.metodosPago.length) {
      return false;
    }

    return this.pagos.some(
      pago => Number(pago.monto) >= 0.02
    );
  }

  // Comprueba si un método ya está siendo utilizado en otro pago.
  metodoYaSeleccionado(
    metodo: MetodoPago,
    indiceActual: number
  ): boolean {

    return this.pagos.some(
      (pago, indice) =>
        indice !== indiceActual &&
        pago.metodoPago === metodo
    );
  }

  // Comprueba si todos los pagos están correctamente distribuidos.
  get pagosValidos(): boolean {

    if (!this.previsualizacion()?.stockSuficiente) {
      return false;
    }

    if (this.pagos.length === 0) {
      return false;
    }

    if (this.excesoPago > 0.001 || this.saldoPendiente > 0.001) {
      return false;
    }

    // Evita utilizar el mismo método más de una vez.
    const metodosSeleccionados = this.pagos.map(
      pago => pago.metodoPago
    );

    if (
      new Set(metodosSeleccionados).size !==
      metodosSeleccionados.length
    ) {
      return false;
    }

    return this.pagos.every(pago => {

      const monto = Number(pago.monto);

      if (!Number.isFinite(monto) || monto <= 0) {
        return false;
      }

      if (pago.metodoPago === 'EFECTIVO') {

        const recibido = Number(pago.montoRecibido);

        if (
          !Number.isFinite(recibido) ||
          recibido < monto
        ) {
          return false;
        }
      }

      return true;
    });
  }

  // Mensaje que explica el estado actual de los pagos.
  get mensajeEstadoPagos(): string {

    if (this.pagos.length === 0) {
      return 'Selecciona al menos un método de pago.';
    }

    if (
      this.pagos.some(
        pago =>
          !Number.isFinite(Number(pago.monto)) ||
          Number(pago.monto) <= 0
      )
    ) {
      return 'Ingresa un importe válido para cada método de pago.';
    }

    if (this.excesoPago > 0.001) {
      return 'Los importes superan el total de la venta. Ajusta las cantidades.';
    }

    if (this.saldoPendiente > 0.001) {
      return `Falta distribuir S/ ${this.saldoPendiente.toFixed(2)} del total de la venta.`;
    }

    if (
      this.pagos.some(
        pago =>
          pago.metodoPago === 'EFECTIVO' &&
          (
            !Number.isFinite(Number(pago.montoRecibido)) ||
            Number(pago.montoRecibido) < Number(pago.monto)
          )
      )
    ) {
      return 'El dinero recibido en efectivo debe cubrir el importe indicado.';
    }

    if (!this.pagosValidos) {
      return 'Revisa los métodos de pago antes de continuar.';
    }

    return 'El pago está completo y listo para continuar.';
  }

  // Inicializa un pago en efectivo con el total de la venta.
  inicializarPagos(): void {

    const total = this.totalVenta;

    this.pagos = [{
      metodoPago: 'EFECTIVO',
      monto: total,
      montoRecibido: total
    }];

    this.errorPagos = '';
  }

  // Divide el importe de un método para agregar otro método de pago.
  agregarPago(): void {

    if (this.pagos.length >= this.metodosPago.length) {

      this.errorPagos =
        'Ya se agregaron todos los métodos de pago disponibles.';

      return;
    }

    // Busca un método que todavía no esté seleccionado.
    const metodoDisponible = this.metodosPago.find(
      metodo =>
        !this.pagos.some(
          pago => pago.metodoPago === metodo.valor
        )
    );

    if (!metodoDisponible) {
      this.errorPagos = 'No hay otros métodos de pago disponibles.';
      return;
    }

    // Busca el pago con mayor importe para dividirlo.
    const pagosOrdenados = this.pagos
      .map((pago, indice) => ({
        pago,
        indice,
        monto: Number(pago.monto) || 0
      }))
      .filter(item => item.monto >= 0.02)
      .sort((a, b) => b.monto - a.monto);

    const pagoSeleccionado = pagosOrdenados[0];

    if (!pagoSeleccionado) {
      this.errorPagos =
        'No hay ningún importe suficiente para dividir el pago.';

      return;
    }

    const pagoOriginal = pagoSeleccionado.pago;
    const montoOriginal = pagoSeleccionado.monto;

    // Divide el importe en dos partes, manteniendo los decimales.
    const montoNuevo = Number(
      (montoOriginal / 2).toFixed(2)
    );

    const montoRestante = Number(
      (montoOriginal - montoNuevo).toFixed(2)
    );

    // Comprueba si el efectivo recibido era igual al importe anterior.
    const recibidoCoincideConMonto =
      Math.abs(
        Number(pagoOriginal.montoRecibido) - montoOriginal
      ) < 0.001;

    pagoOriginal.monto = montoRestante;

    // Si el efectivo recibido era exactamente el importe anterior,
    // también se ajusta automáticamente.
    if (
      pagoOriginal.metodoPago === 'EFECTIVO' &&
      recibidoCoincideConMonto
    ) {
      pagoOriginal.montoRecibido = montoRestante;
    }

    this.pagos.push({
      metodoPago: metodoDisponible.valor,
      monto: montoNuevo,
      montoRecibido: null
    });

    this.errorPagos = '';
  }

  // Elimina un método de pago.
  eliminarPago(indice: number): void {

    if (this.pagos.length <= 1) {

      this.errorPagos =
        'Debe existir al menos un método de pago.';

      return;
    }

    const pagoEliminado = this.pagos[indice];

    if (!pagoEliminado) {
      return;
    }

    const montoEliminado = Number(pagoEliminado.monto) || 0;

    this.pagos.splice(indice, 1);

    // Si solo queda un método, vuelve a cubrir el total completo.
    if (this.pagos.length === 1) {

      const pagoRestante = this.pagos[0];
      const montoAnterior = Number(pagoRestante.monto) || 0;
      const recibidoAnterior = Number(pagoRestante.montoRecibido) || 0;

      const recibidoCoincideConMonto =
        Math.abs(recibidoAnterior - montoAnterior) < 0.001;

      pagoRestante.monto = this.totalVenta;

      if (pagoRestante.metodoPago === 'EFECTIVO') {

        if (recibidoCoincideConMonto) {
          pagoRestante.montoRecibido = this.totalVenta;
        }

      } else {

        pagoRestante.montoRecibido = null;
      }

    } else {

      // Cuando quedan varios métodos, el importe eliminado
      // se suma al pago anterior o al primero disponible.
      const indiceDestino = indice > 0 ? indice - 1 : 0;

      const pagoDestino = this.pagos[indiceDestino];

      if (pagoDestino) {

        const montoAnterior = Number(pagoDestino.monto) || 0;
        const recibidoAnterior = Number(pagoDestino.montoRecibido) || 0;

        const recibidoCoincideConMonto =
          Math.abs(recibidoAnterior - montoAnterior) < 0.001;

        const nuevoMonto = Number(
          (montoAnterior + montoEliminado).toFixed(2)
        );

        pagoDestino.monto = nuevoMonto;

        if (
          pagoDestino.metodoPago === 'EFECTIVO' &&
          recibidoCoincideConMonto
        ) {
          pagoDestino.montoRecibido = nuevoMonto;
        }
      }
    }

    this.errorPagos = '';
  }

  // Cambia el método de pago.
  cambiarMetodoPago(
    indice: number,
    metodo: MetodoPago
  ): void {

    const pago = this.pagos[indice];

    if (!pago) {
      return;
    }

    // No permite repetir un método en otro pago.
    if (this.metodoYaSeleccionado(metodo, indice)) {

      this.errorPagos =
        'Ese método de pago ya está seleccionado.';

      return;
    }

    pago.metodoPago = metodo;

    if (metodo === 'EFECTIVO') {

      pago.montoRecibido = Number(pago.monto) || 0;

    } else {

      pago.montoRecibido = null;
    }

    this.errorPagos = '';
  }

  // Actualiza el importe destinado a un método de pago.
  actualizarMontoPago(
    indice: number,
    valor: number
  ): void {

    const pago = this.pagos[indice];

    if (!pago) {
      return;
    }

    const montoAnterior = Number(pago.monto) || 0;
    const recibidoAnterior = Number(pago.montoRecibido) || 0;

    const recibidoCoincideConMonto =
      Math.abs(recibidoAnterior - montoAnterior) < 0.001;

    const nuevoMonto = Number(valor);

    pago.monto = Number.isFinite(nuevoMonto)
      ? nuevoMonto
      : 0;

    // Si el efectivo recibido se había dejado igual al importe,
    // se mantiene sincronizado con el nuevo importe.
    if (
      pago.metodoPago === 'EFECTIVO' &&
      recibidoCoincideConMonto
    ) {
      pago.montoRecibido = pago.monto;
    }

    this.errorPagos = '';
  }

  // Actualiza el dinero recibido en efectivo.
  actualizarMontoRecibido(
    indice: number,
    valor: number
  ): void {

    const pago = this.pagos[indice];

    if (!pago || pago.metodoPago !== 'EFECTIVO') {
      return;
    }

    const recibido = Number(valor);

    pago.montoRecibido = Number.isFinite(recibido)
      ? recibido
      : 0;

    this.errorPagos = '';
  }

  // Calcula el vuelto de un pago en efectivo.
  calcularVuelto(pago: PagoFormulario): number {

    if (pago.metodoPago !== 'EFECTIVO') {
      return 0;
    }

    const recibido = Number(pago.montoRecibido) || 0;
    const monto = Number(pago.monto) || 0;

    return Number(
      Math.max(0, recibido - monto).toFixed(2)
    );
  }

  // Suma el vuelto de todos los pagos en efectivo.
  get vueltoTotal(): number {

    return Number(
      this.pagos.reduce(
        (total, pago) => total + this.calcularVuelto(pago),
        0
      ).toFixed(2)
    );
  }

  // Construye los pagos que se enviarán al registrar la venta.
  obtenerPagosRequest(): PagoVentaRequest[] | null {

    this.errorPagos = '';

    if (!this.previsualizacion()) {

      this.errorPagos =
        'Primero debes calcular el total de la venta.';

      return null;
    }

    if (!this.previsualizacion()!.stockSuficiente) {

      this.errorPagos =
        'Hay productos sin existencias suficientes. Corrige las cantidades y vuelve a calcular.';

      return null;
    }

    if (!this.pagosValidos) {
      this.errorPagos = this.mensajeEstadoPagos;
      return null;
    }

    const pagosRequest: PagoVentaRequest[] = [];

    for (const pago of this.pagos) {

      const monto = Number(pago.monto);

      if (pago.metodoPago === 'EFECTIVO') {

        const recibido = Number(pago.montoRecibido);

        pagosRequest.push({
          metodoPago: pago.metodoPago,
          monto: Number(monto.toFixed(2)),
          montoRecibido: Number(recibido.toFixed(2))
        });

      } else {

        pagosRequest.push({
          metodoPago: pago.metodoPago,
          monto: Number(monto.toFixed(2)),
          montoRecibido: null
        });
      }
    }

    return pagosRequest;
  }

  // ============================================================
  // UTILIDADES
  // ============================================================

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    const mensaje = String(
      err?.error?.mensaje ??
      err?.error?.message ??
      (typeof err?.error === 'string' ? err.error : '')
    ).trim();

    if (mensaje) {
      return mensaje;
    }

    if (err?.status === 0) {
      return 'No se pudo completar la operación. Comprueba tu conexión e inténtalo nuevamente.';
    }

    if (err?.status === 403) {
      return 'Tu perfil no tiene permisos para realizar esta operación.';
    }

    return mensajePredeterminado;
  }

  // ============================================================
  // NOTIFICACIONES TEMPORALES
  // ============================================================

  private mostrarMensaje(
    mensaje: string,
    tipo: 'success' | 'warning'
  ): void {

    if (this.mensajeTimeout) {
      clearTimeout(this.mensajeTimeout);
    }

    this.mensajeCarrito.set(mensaje);
    this.tipoMensaje.set(tipo);

    this.mensajeTimeout = setTimeout(() => {

      this.mensajeCarrito.set('');
      this.mensajeTimeout = null;

    }, 2500);
  }

  private limpiarMensaje(): void {

    if (this.mensajeTimeout) {
      clearTimeout(this.mensajeTimeout);
      this.mensajeTimeout = null;
    }

    this.mensajeCarrito.set('');
  }


  // ============================================================
  // LIMPIAR DATOS DEL CLIENTE
  // ============================================================

  private limpiarDatosCliente(): void {

    // Limpiar búsqueda de cliente existente.
    this.documentoBusqueda = '';
    this.clienteSeleccionado = null;
    this.mensajeBusquedaCliente = '';
    this.tipoMensajeBusqueda = '';

    // Limpiar todos los datos del cliente nuevo.
    this.nuevoCliente = {
      tipoDocumento: 'DNI',
      numeroDocumento: '',
      nombres: '',
      apellidos: '',
      correo: ''
    };

    // Restablecer modalidad de registro.
    this.modoRegistroNuevo = 'AUTO';

    // Limpiar validaciones anteriores.
    this.errorNuevoCliente = '';
  }


}


