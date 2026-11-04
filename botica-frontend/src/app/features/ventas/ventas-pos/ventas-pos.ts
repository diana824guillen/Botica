import {
  ChangeDetectionStrategy,
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

import { ProductoService } from '../../productos/services/producto';

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

  // ============================================================
  // DATOS REACTIVOS DEL CATÁLOGO
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
  // CARRITO
  // ============================================================

  carrito: ItemCarrito[] = [];

  // Notificaciones reactivas.
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

          // Cerrar el producto desplegado si ya no está disponible.
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

    const stock = Number(
      presentacion.stockDisponible ?? 0
    );

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

    this.limpiarMensaje();
  }

  quitarProducto(idPresentacion: number): void {

    this.carrito = this.carrito.filter(
      item => item.idPresentacion !== idPresentacion
    );

    this.limpiarMensaje();
  }

  limpiarCarrito(): void {

    this.carrito = [];

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
}


