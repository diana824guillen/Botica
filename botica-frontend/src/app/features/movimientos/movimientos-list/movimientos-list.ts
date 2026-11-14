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
  Subscription,
  debounceTime,
  distinctUntilChanged,
  finalize
} from 'rxjs';

import { MovimientoInventarioService } from '../services/movimiento-inventario';

import {
  MovimientoInventario,
  TipoMovimientoInventario
} from '../../../core/models/movimiento-inventario.model';

@Component({
  selector: 'app-movimientos-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './movimientos-list.html',
  styleUrls: ['./movimientos-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MovimientosListComponent implements OnInit, OnDestroy {

  private movimientoService = inject(MovimientoInventarioService);
  private cdr = inject(ChangeDetectorRef);

  private searchSubject = new Subject<string>();

  private searchSubscription?: Subscription;
  private movimientosSubscription?: Subscription;

  private destruido = false;

  // ============================================================
  // LISTADO
  // ============================================================

  movimientos: MovimientoInventario[] = [];

  isLoading = false;
  errorCarga = '';

  // ============================================================
  // BÚSQUEDA Y FILTROS
  // ============================================================

  terminoBusqueda = '';

  filtroTipo: TipoMovimientoInventario | '' = '';

  fechaDesde = '';
  fechaHasta = '';

  // ============================================================
  // PAGINACIÓN
  // ============================================================

  currentPage = 0;

  // Se mantiene el mismo tamaño de página que Historial de ventas.
  readonly pageSize = 10;

  totalElements = 0;
  totalPages = 0;

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

        this.cargarMovimientos();
      });

    this.cargarMovimientos();
  }

  ngOnDestroy(): void {

    this.destruido = true;

    this.searchSubscription?.unsubscribe();

    this.movimientosSubscription?.unsubscribe();

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
  // CARGAR MOVIMIENTOS
  // ============================================================

  cargarMovimientos(): void {

    // Cancelar cualquier consulta anterior.
    this.movimientosSubscription?.unsubscribe();

    this.isLoading = true;
    this.errorCarga = '';

    this.actualizarVista();

    this.movimientosSubscription = this.movimientoService
      .listarMovimientos(
        this.currentPage,
        this.pageSize,
        this.terminoBusqueda,
        this.filtroTipo,
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

          this.movimientos = respuesta.content ?? [];

          this.totalElements = respuesta.totalElements ?? 0;
          this.totalPages = respuesta.totalPages ?? 0;

          this.currentPage = respuesta.number ?? this.currentPage;

          this.actualizarVista();
        },

        error: err => {

          console.error(
            'Error al cargar los movimientos de inventario:',
            err
          );

          this.movimientos = [];

          this.totalElements = 0;
          this.totalPages = 0;

          if (err?.status === 403) {

            this.errorCarga =
              'Tu perfil no tiene autorización para consultar los movimientos de inventario.';

          } else if (err?.status === 0) {

            this.errorCarga =
              'No se pudo conectar con el servidor.';

          } else {

            this.errorCarga =
              err?.error?.mensaje ||
              err?.error?.message ||
              'No se pudieron cargar los movimientos de inventario.';
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

    this.cargarMovimientos();
  }

  // ============================================================
  // LIMPIAR FILTROS
  // ============================================================

  limpiarFiltros(): void {

    this.terminoBusqueda = '';

    this.filtroTipo = '';

    this.fechaDesde = '';
    this.fechaHasta = '';

    this.currentPage = 0;

    // Sincronizar el buscador y limpiar búsquedas pendientes.
    this.searchSubject.next('');

    this.cargarMovimientos();
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

    this.cargarMovimientos();
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
  // NOMBRE DEL TIPO DE MOVIMIENTO
  // ============================================================

  obtenerNombreTipo(tipo: TipoMovimientoInventario): string {

    const nombres: Record<TipoMovimientoInventario, string> = {

      INGRESO: 'Ingreso',

      SALIDA_VENTA: 'Salida por venta',

      AJUSTE: 'Ajuste',

      MERMA: 'Merma',

      DEVOLUCION: 'Devolución'
    };

    return nombres[tipo] || tipo;
  }

  // ============================================================
  // CLASE VISUAL DEL TIPO DE MOVIMIENTO
  // ============================================================

  obtenerClaseTipo(tipo: TipoMovimientoInventario): string {

    switch (tipo) {

      case 'INGRESO':
        return 'movement-income';

      case 'SALIDA_VENTA':
        return 'movement-sale';

      case 'AJUSTE':
        return 'movement-adjustment';

      case 'MERMA':
        return 'movement-loss';

      case 'DEVOLUCION':
        return 'movement-return';

      default:
        return 'movement-unknown';
    }
  }

  // ============================================================
  // NOMBRE DE LA PRESENTACIÓN
  // ============================================================

  obtenerNombrePresentacion(tipo: string | null): string {

    if (!tipo) {
      return 'No disponible';
    }

    const nombres: Record<string, string> = {

      UNIDAD: 'unidad',

      BLISTER: 'blíster',

      CAJA: 'caja',

      FRASCO: 'frasco',

      TIRA: 'tira',

      OTRO: 'otra presentación'
    };

    return nombres[tipo] || tipo;
  }

  // ============================================================
  // CLASE DE LA VARIACIÓN DE EXISTENCIAS
  // ============================================================

  obtenerClaseCantidad(cantidad: number): string {

    if (cantidad > 0) {
      return 'quantity-positive';
    }

    if (cantidad < 0) {
      return 'quantity-negative';
    }

    return 'quantity-neutral';
  }

  // ============================================================
  // MENSAJE DE ERROR
  // ============================================================

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    if (err?.status === 403) {
      return 'Tu perfil no tiene autorización para consultar los movimientos de inventario.';
    }

    return err?.error?.mensaje ||
      err?.error?.message ||
      mensajePredeterminado;
  }

}


