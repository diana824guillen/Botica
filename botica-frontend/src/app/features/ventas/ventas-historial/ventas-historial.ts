import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  Subject,
  debounceTime,
  distinctUntilChanged,
  finalize
} from 'rxjs';

import { VentaService } from '../services/venta';

import {
  MetodoPago,
  VentaListado,
  VentaResponse
} from '../../../core/models/venta.model';

@Component({
  selector: 'app-ventas-historial',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './ventas-historial.html',
  styleUrls: ['./ventas-historial.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VentasHistorialComponent implements OnInit, OnDestroy {

  private ventaService = inject(VentaService);
  private cdr = inject(ChangeDetectorRef);

  private searchSubject = new Subject<string>();

  // ============================================================
  // LISTADO DE VENTAS
  // ============================================================

  ventas: VentaListado[] = [];

  isLoading = false;
  errorCarga = '';

  // ============================================================
  // BÚSQUEDA Y FILTROS
  // ============================================================

  terminoBusqueda = '';

  fechaDesde = '';
  fechaHasta = '';

  // ============================================================
  // PAGINACIÓN
  // ============================================================

  currentPage = 0;
  pageSize = 10;

  totalElements = 0;
  totalPages = 0;

  readonly tamanosPagina = [10, 20, 50];

  // ============================================================
  // DETALLE DE LA VENTA
  // ============================================================

  mostrarDetalle = false;

  cargandoDetalle = false;
  errorDetalle = '';

  ventaSeleccionada: VentaResponse | null = null;

  // ============================================================
  // CICLO DE VIDA
  // ============================================================

  ngOnInit(): void {

    // Evitar consultar el servidor por cada tecla escrita.
    this.searchSubject
      .pipe(
        debounceTime(350),
        distinctUntilChanged()
      )
      .subscribe(termino => {

        this.terminoBusqueda = termino;
        this.currentPage = 0;

        this.cargarVentas();
      });

    this.cargarVentas();
  }

  ngOnDestroy(): void {

    this.searchSubject.complete();
  }

  // ============================================================
  // CARGAR HISTORIAL
  // ============================================================

  cargarVentas(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.ventaService
      .listarVentas(
        this.currentPage,
        this.pageSize,
        this.terminoBusqueda,
        this.fechaDesde,
        this.fechaHasta
      )
      .pipe(
        finalize(() => {

          this.isLoading = false;

          this.cdr.markForCheck();
        })
      )
      .subscribe({

        next: respuesta => {

          this.ventas = respuesta.content || [];

          this.totalElements = respuesta.totalElements || 0;
          this.totalPages = respuesta.totalPages || 0;

          this.currentPage = respuesta.number ?? this.currentPage;

          this.cdr.markForCheck();
        },

        error: err => {

          console.error(
            'Error al consultar el historial de ventas:',
            err
          );

          this.ventas = [];
          this.totalElements = 0;
          this.totalPages = 0;

          if (err?.status === 403) {

            this.errorCarga =
              'Tu perfil no tiene autorización para consultar el historial de ventas.';

          } else if (err?.status === 0) {

            this.errorCarga =
              'No se pudo conectar con el servidor.';

          } else {

            this.errorCarga =
              err?.error?.mensaje ||
              err?.error?.message ||
              'No se pudo cargar el historial de ventas.';
          }

          this.cdr.markForCheck();
        }
      });
  }

  // ============================================================
  // BÚSQUEDA
  // ============================================================

  onSearchInput(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.terminoBusqueda = input.value || '';

    this.searchSubject.next(this.terminoBusqueda);
  }

  // ============================================================
  // APLICAR FILTROS
  // ============================================================

  aplicarFiltros(): void {

    if (
      this.fechaDesde &&
      this.fechaHasta &&
      this.fechaDesde > this.fechaHasta
    ) {

      this.errorCarga =
        'La fecha inicial no puede ser posterior a la fecha final.';

      this.cdr.markForCheck();

      return;
    }

    this.errorCarga = '';
    this.currentPage = 0;

    this.cargarVentas();
  }

  // ============================================================
  // LIMPIAR FILTROS
  // ============================================================

  limpiarFiltros(): void {

    this.terminoBusqueda = '';

    this.fechaDesde = '';
    this.fechaHasta = '';

    this.currentPage = 0;

    // Actualizar también el flujo de búsqueda pendiente.
    this.searchSubject.next('');

    this.cargarVentas();
  }

  // ============================================================
  // CAMBIAR TAMAÑO DE PÁGINA
  // ============================================================

  cambiarTamanoPagina(valor: number | string): void {

    const tamano = Number(valor);

    if (!this.tamanosPagina.includes(tamano)) {
      return;
    }

    this.pageSize = tamano;
    this.currentPage = 0;

    this.cargarVentas();
  }

  // ============================================================
  // CAMBIAR PÁGINA
  // ============================================================

  cambiarPagina(pagina: number): void {

    if (
      pagina < 0 ||
      pagina >= this.totalPages ||
      this.isLoading
    ) {
      return;
    }

    this.currentPage = pagina;

    this.cargarVentas();
  }

  // ============================================================
  // RANGO DE REGISTROS MOSTRADOS
  // ============================================================

  get primerRegistro(): number {

    if (this.totalElements === 0) {
      return 0;
    }

    return (this.currentPage * this.pageSize) + 1;
  }

  get ultimoRegistro(): number {

    return Math.min(
      (this.currentPage + 1) * this.pageSize,
      this.totalElements
    );
  }

  // ============================================================
  // CONSULTAR DETALLE DE UNA VENTA
  // ============================================================

  abrirDetalle(venta: VentaListado): void {

    this.mostrarDetalle = true;

    this.cargandoDetalle = true;
    this.errorDetalle = '';

    this.ventaSeleccionada = null;

    this.ventaService
      .obtenerVentaPorId(venta.id)
      .pipe(
        finalize(() => {

          this.cargandoDetalle = false;

          this.cdr.markForCheck();
        })
      )
      .subscribe({

        next: respuesta => {

          this.ventaSeleccionada = respuesta;

          this.cdr.markForCheck();
        },

        error: err => {

          console.error(
            'Error al consultar el detalle de la venta:',
            err
          );

          if (err?.status === 403) {

            this.errorDetalle =
              'No tienes autorización para consultar el detalle de esta venta.';

          } else if (err?.status === 404) {

            this.errorDetalle =
              'La venta solicitada ya no se encuentra disponible.';

          } else if (err?.status === 0) {

            this.errorDetalle =
              'No se pudo conectar con el servidor.';

          } else {

            this.errorDetalle =
              err?.error?.mensaje ||
              err?.error?.message ||
              'No se pudo recuperar el detalle de la venta.';
          }

          this.cdr.markForCheck();
        }
      });
  }

  // ============================================================
  // CERRAR DETALLE
  // ============================================================

  cerrarDetalle(): void {

    if (this.cargandoDetalle) {
      return;
    }

    this.mostrarDetalle = false;

    this.ventaSeleccionada = null;
    this.errorDetalle = '';
  }

  // ============================================================
  // FORMATO DE ESTADO
  // ============================================================

  obtenerNombreEstado(estado: number): string {

    switch (estado) {

      case 1:
        return 'Emitida';

      case 0:
        return 'Anulada';

      case 2:
        return 'Eliminada';

      default:
        return 'Desconocido';
    }
  }

  obtenerClaseEstado(estado: number): string {

    switch (estado) {

      case 1:
        return 'status-issued';

      case 0:
        return 'status-cancelled';

      case 2:
        return 'status-deleted';

      default:
        return 'status-unknown';
    }
  }

  // ============================================================
  // FORMATO DE DOCUMENTO
  // ============================================================

  obtenerDocumento(
    tipoDocumento: string | null,
    numeroDocumento: string | null
  ): string {

    if (!numeroDocumento) {
      return 'Sin documento registrado';
    }

    return tipoDocumento
      ? `${tipoDocumento}: ${numeroDocumento}`
      : numeroDocumento;
  }

  // ============================================================
  // FORMATO DE MÉTODOS DE PAGO
  // ============================================================

  obtenerNombreMetodoPago(metodo: MetodoPago): string {

    const nombres: Record<MetodoPago, string> = {

      EFECTIVO: 'Efectivo',
      YAPE: 'Yape',
      PLIN: 'Plin',
      TARJETA: 'Tarjeta',
      TRANSFERENCIA: 'Transferencia'
    };

    return nombres[metodo] || metodo;
  }

}


