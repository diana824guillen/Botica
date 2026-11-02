import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import { Descuento } from '../../../core/models/descuento.model';

import { HasPermissionDirective } from '../../../core/directives/has-permission';

import { DescuentoService } from '../services/descuento';

import { DescuentoModalComponent } from '../descuento-modal/descuento-modal';

type TipoConfirmacionDescuento =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionDescuento {
  id: number;
  tipo: TipoConfirmacionDescuento;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-descuentos-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    DescuentoModalComponent
  ],
  templateUrl: './descuentos-list.html',
  styleUrls: ['./descuentos-list.scss']
})
export class DescuentosListComponent implements OnInit {

  descuentos: Descuento[] = [];

  isLoading = true;
  errorCarga = '';

  mostrarModal = false;
  descuentoSeleccionado: Descuento | null = null;

  searchTerm = '';

  currentPage = 0;
  pageSize = 10;

  confirmacion: ConfirmacionDescuento | null = null;
  procesandoConfirmacion = false;
  errorConfirmacion = '';

  private descuentoService = inject(DescuentoService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.cargarDescuentos();
  }

  // ============================================================
  // BÚSQUEDA Y PAGINACIÓN
  // ============================================================

  get descuentosFiltrados(): Descuento[] {

    const termino = this.searchTerm.trim().toLowerCase();

    if (!termino) {
      return this.descuentos;
    }

    return this.descuentos.filter(descuento => {

      const nombre = descuento.nombre.toLowerCase();

      const producto = (
        descuento.productoNombre || ''
      ).toLowerCase();

      const condicion = this.obtenerTipoCondicion(
        descuento.tipoCondicion
      ).toLowerCase();

      return nombre.includes(termino)
        || producto.includes(termino)
        || condicion.includes(termino);
    });
  }

  get totalPages(): number {

    return Math.ceil(
      this.descuentosFiltrados.length / this.pageSize
    );
  }

  get descuentosPaginados(): Descuento[] {

    const inicio = this.currentPage * this.pageSize;

    return this.descuentosFiltrados.slice(
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

  // ============================================================
  // CARGAR DESCUENTOS
  // ============================================================

  cargarDescuentos(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.descuentoService
      .listarDescuentos()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.descuentos = respuesta || [];
          this.ajustarPagina();
        },

        error: (err) => {

          console.error('Error al cargar promociones:', err);

          this.descuentos = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar las promociones.'
          );
        }

      });
  }

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje
      ?? cuerpo?.message
      ?? (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    if (err?.status === 403) {
      return mensaje
        || 'No tiene permisos para realizar esta operación.';
    }

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor. Inténtelo nuevamente.';
    }

    return mensaje || mensajePredeterminado;
  }

  // ============================================================
  // FORMATO DE INFORMACIÓN
  // ============================================================

  obtenerTipoCondicion(tipo: string): string {

    switch (tipo) {
      case 'MONTO_MINIMO':
        return 'Monto mínimo de compra';

      case 'CANTIDAD_MINIMA':
        return 'Cantidad mínima de unidades';

      default:
        return tipo;
    }
  }

  obtenerDetalleCondicion(descuento: Descuento): string {

    const valor = Number(descuento.valorCondicion).toLocaleString(
      'es-PE',
      {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    );

    if (descuento.tipoCondicion === 'MONTO_MINIMO') {
      return `Compra mínima: S/ ${valor}`;
    }

    if (descuento.tipoCondicion === 'CANTIDAD_MINIMA') {
      return `Mínimo: ${valor} unidades base`;
    }

    return valor;
  }

  formatearFecha(fecha: string | null): string {

    if (!fecha) {
      return 'Sin límite';
    }

    // Evitar desplazamientos de día al convertir fechas sin hora.
    const partes = fecha.split('-');

    if (partes.length !== 3) {
      return fecha;
    }

    return `${partes[2]}/${partes[1]}/${partes[0]}`;
  }

  obtenerVigencia(descuento: Descuento): string {

    if (!descuento.fechaInicio && !descuento.fechaFin) {
      return 'Sin límite de fechas';
    }

    const inicio = descuento.fechaInicio
      ? this.formatearFecha(descuento.fechaInicio)
      : 'Sin inicio';

    const fin = descuento.fechaFin
      ? this.formatearFecha(descuento.fechaFin)
      : 'Sin fin';

    return `${inicio} — ${fin}`;
  }

  // ============================================================
  // ABRIR MODAL
  // ============================================================

  abrirModal(descuento?: Descuento): void {

    this.descuentoSeleccionado = descuento || null;
    this.errorCarga = '';
    this.mostrarModal = true;
  }

  cerrarModal(): void {

    this.mostrarModal = false;
    this.descuentoSeleccionado = null;
  }

  // ============================================================
  // SOLICITAR ACTIVACIÓN O DESACTIVACIÓN
  // ============================================================

  solicitarCambioEstado(
    descuento: Descuento,
    nuevoEstado: number
  ): void {

    const activar = nuevoEstado === 1;

    this.confirmacion = {

      id: descuento.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      nombre: descuento.nombre,

      titulo: activar
        ? 'Activar promoción'
        : 'Desactivar promoción',

      mensaje: activar
        ? '¿Desea activar esta promoción?'
        : '¿Desea desactivar esta promoción? Dejará de estar disponible para las ventas mientras permanezca inactiva.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // SOLICITAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(descuento: Descuento): void {

    this.confirmacion = {

      id: descuento.id,
      tipo: 'eliminar',
      nombre: descuento.nombre,

      titulo: 'Eliminar promoción',

      mensaje:
        '¿Está seguro de eliminar esta promoción? El registro se marcará como eliminado.',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // CERRAR CONFIRMACIÓN
  // ============================================================

  cerrarConfirmacion(): void {

    if (this.procesandoConfirmacion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  // ============================================================
  // EJECUTAR CONFIRMACIÓN
  // ============================================================

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    this.procesandoConfirmacion = true;
    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'
        ? this.descuentoService.eliminarDescuento(accion.id)
        : this.descuentoService.cambiarEstado(
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
          this.cargarDescuentos();
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


