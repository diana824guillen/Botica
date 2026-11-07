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
import { NotaVentaPdfService } from '../services/nota-venta-pdf';

import {
  Subject,
  Subscription,
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
  private notaVentaPdfService = inject(NotaVentaPdfService);

  private searchSubject = new Subject<string>();

  private searchSubscription?: Subscription;
  private ventasSubscription?: Subscription;
  private detalleSubscription?: Subscription;

  private destruido = false;

  // ============================================================
  // LISTADO
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

  // Se mantiene fijo como en el módulo de Clientes.
  readonly pageSize = 10;

  totalElements = 0;
  totalPages = 0;

  // ============================================================
  // DETALLE DE VENTA
  // ============================================================

  mostrarDetalle = false;

  cargandoDetalle = false;
  errorDetalle = '';

  ventaSeleccionada: VentaResponse | null = null;

  // ============================================================
  // CICLO DE VIDA
  // ============================================================

  ngOnInit(): void {

    this.searchSubscription = this.searchSubject
      .pipe(
        debounceTime(400),
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

    this.destruido = true;

    this.searchSubscription?.unsubscribe();

    this.ventasSubscription?.unsubscribe();

    this.detalleSubscription?.unsubscribe();

    this.searchSubject.complete();
  }

  // ============================================================
  // ACTUALIZAR VISTA
  // ============================================================

  private actualizarVista(): void {

    if (!this.destruido) {
      this.cdr.detectChanges();
    }
  }

  // ============================================================
  // CARGAR HISTORIAL
  // ============================================================

  cargarVentas(): void {

    // Cancelar cualquier consulta anterior.
    this.ventasSubscription?.unsubscribe();

    this.isLoading = true;
    this.errorCarga = '';

    this.actualizarVista();

    this.ventasSubscription = this.ventaService
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

          this.actualizarVista();
        })
      )
      .subscribe({

        next: respuesta => {

          this.ventas = respuesta.content ?? [];

          this.totalElements = respuesta.totalElements ?? 0;
          this.totalPages = respuesta.totalPages ?? 0;

          this.currentPage = respuesta.number ?? this.currentPage;

          this.actualizarVista();
        },

        error: err => {

          console.error(
            'Error al cargar el historial de ventas:',
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

          this.actualizarVista();
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

      this.actualizarVista();

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

    // Sincronizar el buscador y cancelar consultas pendientes.
    this.searchSubject.next('');

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
  // RANGO DE REGISTROS
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

    // Cancelar una consulta de detalle anterior.
    this.detalleSubscription?.unsubscribe();

    this.mostrarDetalle = true;

    this.cargandoDetalle = true;
    this.errorDetalle = '';

    this.ventaSeleccionada = null;

    this.actualizarVista();

    this.detalleSubscription = this.ventaService
      .obtenerVentaPorId(venta.id)
      .pipe(
        finalize(() => {

          this.cargandoDetalle = false;

          this.actualizarVista();
        })
      )
      .subscribe({

        next: respuesta => {

          this.ventaSeleccionada = respuesta;

          this.actualizarVista();
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

          this.actualizarVista();
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

    this.detalleSubscription?.unsubscribe();

    this.mostrarDetalle = false;

    this.ventaSeleccionada = null;
    this.errorDetalle = '';

    this.actualizarVista();
  }

  // ============================================================
  // NOTA DE VENTA PDF
  // ============================================================

  descargarNotaA4(venta: VentaResponse): void {
    this.notaVentaPdfService.descargarA4(venta);
  }

  descargarNotaTicket(venta: VentaResponse): void {
    this.notaVentaPdfService.descargarTicket(venta);
  }

  imprimirNotaA4(venta: VentaResponse): void {
    this.notaVentaPdfService.imprimirA4(venta);
  }

  imprimirNotaTicket(venta: VentaResponse): void {
    this.notaVentaPdfService.imprimirTicket(venta);
  }

  // ============================================================
  // ESTADO DE LA VENTA
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
  // DOCUMENTO DEL CLIENTE
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
  // MÉTODOS DE PAGO
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


