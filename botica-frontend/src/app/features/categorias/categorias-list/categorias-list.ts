import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import { Categoria } from '../../../core/models/categoria.model';
import { HasPermissionDirective } from '../../../core/directives/has-permission';
import { CategoriaService } from '../services/categoria';
import { CategoriaModalComponent } from '../categoria-modal/categoria-modal';

type TipoConfirmacionCategoria =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionCategoria {
  id: number;
  tipo: TipoConfirmacionCategoria;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-categorias-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    CategoriaModalComponent
  ],
  templateUrl: './categorias-list.html',
  styleUrls: ['./categorias-list.scss']
})
export class CategoriasListComponent implements OnInit {

  categorias: Categoria[] = [];

  isLoading = true;
  errorCarga = '';

  mostrarModal = false;
  categoriaSeleccionada: Categoria | null = null;

  searchTerm = '';

  currentPage = 0;
  pageSize = 10;

  confirmacion: ConfirmacionCategoria | null = null;
  procesandoConfirmacion = false;
  errorConfirmacion = '';

  private categoriaService = inject(CategoriaService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.cargarCategorias();
  }

  // ==================== BÚSQUEDA Y PAGINACIÓN ====================

  get categoriasFiltradas(): Categoria[] {

    const termino = this.searchTerm.trim().toLowerCase();

    if (!termino) {
      return this.categorias;
    }

    return this.categorias.filter(categoria => {

      const nombre = categoria.nombre.toLowerCase();
      const descripcion = (
        categoria.descripcion || ''
      ).toLowerCase();

      return nombre.includes(termino) ||
        descripcion.includes(termino);
    });
  }

  get totalPages(): number {
    return Math.ceil(
      this.categoriasFiltradas.length / this.pageSize
    );
  }

  get categoriasPaginadas(): Categoria[] {

    const inicio = this.currentPage * this.pageSize;

    return this.categoriasFiltradas.slice(
      inicio,
      inicio + this.pageSize
    );
  }

  onSearchInput(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.searchTerm = input.value;
    this.currentPage = 0;
  }

  cambiarPagina(page: number): void {

    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
    }
  }

  private ajustarPagina(): void {

    const paginas = this.totalPages;

    if (paginas === 0) {
      this.currentPage = 0;
    } else if (this.currentPage >= paginas) {
      this.currentPage = paginas - 1;
    }
  }

  // ==================== CARGAR CATEGORÍAS ====================

  cargarCategorias(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.categoriaService
      .listarCategorias()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {
          this.categorias = response || [];
          this.ajustarPagina();
        },

        error: (err) => {
          console.error('Error al cargar categorías:', err);

          this.categorias = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar las categorías.'
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

  // ==================== ABRIR MODAL ====================

  abrirModal(categoria?: Categoria): void {

    this.categoriaSeleccionada = categoria || null;
    this.mostrarModal = true;
  }

  cerrarModal(): void {

    this.mostrarModal = false;
    this.categoriaSeleccionada = null;
  }

  // ==================== SOLICITAR CAMBIO DE ESTADO ====================

  solicitarCambioEstado(
    categoria: Categoria,
    nuevoEstado: number
  ): void {

    const activar = nuevoEstado === 1;

    this.confirmacion = {

      id: categoria.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      nombre: categoria.nombre,

      titulo: activar
        ? 'Activar categoría'
        : 'Desactivar categoría',

      mensaje: activar
        ? '¿Desea activar esta categoría?'
        : '¿Desea desactivar esta categoría? La categoría se conservará, pero quedará inactiva.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ==================== SOLICITAR ELIMINACIÓN ====================

  solicitarEliminacion(categoria: Categoria): void {

    this.confirmacion = {

      id: categoria.id,

      tipo: 'eliminar',

      nombre: categoria.nombre,

      titulo: 'Eliminar categoría',

      mensaje:
        '¿Está seguro de eliminar esta categoría? El registro se marcará como eliminado.',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  // ==================== CERRAR CONFIRMACIÓN ====================

  cerrarConfirmacion(): void {

    if (this.procesandoConfirmacion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  // ==================== EJECUTAR ACCIÓN ====================

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    this.procesandoConfirmacion = true;
    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'
        ? this.categoriaService.eliminarCategoria(accion.id)
        : this.categoriaService.cambiarEstado(
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
          this.cargarCategorias();
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


