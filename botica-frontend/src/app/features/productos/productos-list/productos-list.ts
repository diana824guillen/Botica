import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import { Producto } from '../../../core/models/producto.model';
import { Categoria } from '../../../core/models/categoria.model';

import { HasPermissionDirective } from '../../../core/directives/has-permission';

import { CategoriaService } from '../../categorias/services/categoria';
import { ProductoService } from '../services/producto';

import { ProductoModalComponent } from '../producto-modal/producto-modal';
import { PresentacionesModalComponent } from '../presentaciones-modal/presentaciones-modal';

type TipoConfirmacionProducto = 'activar' | 'desactivar' | 'eliminar';

interface ConfirmacionProducto {
  id: number;
  tipo: TipoConfirmacionProducto;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-productos-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    ProductoModalComponent,
    PresentacionesModalComponent
  ],
  templateUrl: './productos-list.html',
  styleUrls: ['./productos-list.scss']
})
export class ProductosListComponent implements OnInit {

  productos: Producto[] = [];
  categorias: Categoria[] = [];

  isLoading = true;
  errorCarga = '';

  mostrarModalProducto = false;
  productoSeleccionado: Producto | null = null;

  mostrarModalPresentaciones = false;
  productoParaPresentaciones: Producto | null = null;

  searchTerm = '';
  categoriaSeleccionada: number | null = null;

  currentPage = 0;
  pageSize = 10;

  confirmacion: ConfirmacionProducto | null = null;
  procesandoConfirmacion = false;
  errorConfirmacion = '';

  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarCategoriasFiltro();
  }

  // ==================== BÚSQUEDA Y FILTROS ====================

  get productosFiltrados(): Producto[] {

    const termino = this.searchTerm.trim().toLowerCase();

    return this.productos.filter(producto => {

      const coincideTexto =
        !termino ||
        producto.nombre.toLowerCase().includes(termino) ||
        producto.categoriaNombre.toLowerCase().includes(termino) ||
        (producto.descripcion || '').toLowerCase().includes(termino);

      const coincideCategoria =
        this.categoriaSeleccionada === null ||
        producto.categoriaId === this.categoriaSeleccionada;

      return coincideTexto && coincideCategoria;
    });
  }

  get totalPages(): number {
    return Math.ceil(
      this.productosFiltrados.length / this.pageSize
    );
  }

  get productosPaginados(): Producto[] {

    const inicio = this.currentPage * this.pageSize;

    return this.productosFiltrados.slice(
      inicio,
      inicio + this.pageSize
    );
  }

  onSearchInput(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.searchTerm = input.value;
    this.currentPage = 0;
  }

  cambiarFiltroCategoria(id: number | null): void {

    this.categoriaSeleccionada = id;
    this.currentPage = 0;
  }

  cambiarPagina(page: number): void {

    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
    }
  }

  private ajustarPagina(): void {

    if (this.totalPages === 0) {
      this.currentPage = 0;
    } else if (this.currentPage >= this.totalPages) {
      this.currentPage = this.totalPages - 1;
    }
  }

  // ==================== CARGA DE INFORMACIÓN ====================

  cargarCategoriasFiltro(): void {

    this.categoriaService.listarCategorias().subscribe({

      next: (respuesta) => {
        this.categorias = respuesta || [];
      },

      error: (err) => {
        console.error('Error al cargar categorías del filtro:', err);
      }

    });
  }

  cargarProductos(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.productoService
      .listarProductos()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.ajustarPagina();
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.productos = respuesta || [];
          this.ajustarPagina();
        },

        error: (err) => {

          console.error('Error al cargar productos:', err);

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los productos.'
          );
        }

      });
  }

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    if (err?.status === 403) {
      return err?.error?.mensaje ||
        'No tiene permisos para realizar esta operación.';
    }

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    return err?.error?.mensaje ||
      err?.error?.message ||
      mensajePredeterminado;
  }

  // ==================== MOSTRAR PRESENTACIONES ====================

  resumenPresentaciones(producto: Producto): string {

    const presentaciones = (producto.presentaciones || [])
      .filter(p => p.estado === 1);

    if (presentaciones.length === 0) {
      return 'Sin presentaciones activas';
    }

    return presentaciones.map(p =>
      `${this.etiquetaTipo(p.tipo)} (${p.factorConversion}) — S/ ${Number(p.precioVenta).toFixed(2)}`
    ).join(' · ');
  }

  private etiquetaTipo(tipo: string): string {

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

  // ==================== MODAL DE PRODUCTO ====================

  abrirModalProducto(producto?: Producto): void {

    this.productoSeleccionado = producto || null;
    this.mostrarModalProducto = true;
  }

  cerrarModalProducto(): void {

    this.mostrarModalProducto = false;
    this.productoSeleccionado = null;
  }

  // ==================== MODAL DE PRESENTACIONES ====================

  abrirModalPresentaciones(producto: Producto): void {

    this.productoParaPresentaciones = producto;
    this.mostrarModalPresentaciones = true;
  }

  cerrarModalPresentaciones(): void {

    this.mostrarModalPresentaciones = false;
    this.productoParaPresentaciones = null;
  }

  // ==================== CONFIRMAR ACTIVAR / DESACTIVAR ====================

  solicitarCambioEstado(
    producto: Producto,
    nuevoEstado: number
  ): void {

    const activar = nuevoEstado === 1;

    this.confirmacion = {

      id: producto.id,

      tipo: activar ? 'activar' : 'desactivar',

      nombre: producto.nombre,

      titulo: activar
        ? 'Activar producto'
        : 'Desactivar producto',

      mensaje: activar
        ? '¿Desea activar este producto para que pueda utilizarse nuevamente?'
        : '¿Desea desactivar este producto? El registro se conservará, pero no estará disponible como producto activo.',

      textoBoton: activar ? 'Activar' : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ==================== CONFIRMAR ELIMINACIÓN ====================

  solicitarEliminacion(producto: Producto): void {

    this.confirmacion = {

      id: producto.id,

      tipo: 'eliminar',

      nombre: producto.nombre,

      titulo: 'Eliminar producto',

      mensaje:
        '¿Está seguro de eliminar este producto? Se marcará como eliminado y dejará de aparecer en el listado.',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  cerrarConfirmacion(): void {

    if (this.procesandoConfirmacion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    this.procesandoConfirmacion = true;
    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'

        ? this.productoService.eliminarProducto(accion.id)

        : this.productoService.cambiarEstado(
            accion.id,
            accion.tipo === 'activar' ? 1 : 0
          );

    peticion
      .pipe(
        finalize(() => {
          this.procesandoConfirmacion = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {
          this.confirmacion = null;
          this.cargarProductos();
        },

        error: (err) => {
          this.errorConfirmacion = this.obtenerMensajeError(
            err,
            'No se pudo completar la acción. Inténtelo nuevamente.'
          );
        }

      });
  }
}


