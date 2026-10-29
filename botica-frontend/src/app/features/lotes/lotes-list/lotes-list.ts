import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, finalize } from 'rxjs';

import { LoteService } from '../services/lote';
import { Lote } from '../../../core/models/lote.model';
import { HasPermissionDirective } from '../../../core/directives/has-permission';
import { LoteModalComponent } from '../lote-modal/lote-modal';

type FiltroEstado = 'todos' | 'activo' | 'inactivo';
type FiltroVencimiento = 'todos' | 'vencidos' | 'rango';

type TipoConfirmacionLote =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionLote {
  id: number;
  tipo: TipoConfirmacionLote;
  numeroLote: string;
  nombreProducto: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-lotes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    LoteModalComponent
  ],
  templateUrl: './lotes-list.html',
  styleUrls: ['./lotes-list.scss']
})
export class LotesListComponent implements OnInit {

  private loteService = inject(LoteService);
  private cdr = inject(ChangeDetectorRef);

  // ============================================================
  // DATOS PRINCIPALES
  // ============================================================

  lotes: Lote[] = [];

  cargandoLotes = false;
  procesando = false;

  errorCarga = '';

  // ============================================================
  // BÚSQUEDA Y FILTROS
  // ============================================================

  busqueda = '';

  filtroEstado: FiltroEstado = 'todos';

  filtroVencimiento: FiltroVencimiento = 'todos';

  fechaDesde = '';
  fechaHasta = '';

  // ============================================================
  // MODALES DE INGRESO Y AJUSTE
  // ============================================================

  mostrarModalIngreso = false;

  mostrarModalAjuste = false;

  loteAjuste: Lote | null = null;

  // ============================================================
  // CONFIRMACIÓN DE ACTIVACIÓN, DESACTIVACIÓN Y ELIMINACIÓN
  // ============================================================

  confirmacionLote: ConfirmacionLote | null = null;

  errorConfirmacionLote = '';

  ngOnInit(): void {
    this.cargarLotes();
  }

  // ============================================================
  // FECHAS
  // ============================================================

  get fechaHoy(): string {
    return this.convertirFechaAString(new Date());
  }

  private convertirFechaAString(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  get rangoFechasValido(): boolean {
    return !!this.fechaDesde &&
      !!this.fechaHasta &&
      this.fechaDesde <= this.fechaHasta;
  }

  cambiarFiltroVencimiento(tipo: FiltroVencimiento): void {
    this.filtroVencimiento = tipo;

    // Las fechas anteriores no deben mantenerse al cambiar de filtro.
    this.fechaDesde = '';
    this.fechaHasta = '';
  }

  // ============================================================
  // LISTADO Y ORDEN VISUAL
  // ============================================================

  get lotesOrdenados(): Lote[] {
    return [...this.lotes].sort((a, b) => {
      const fechaA = a.fechaIngreso || '';
      const fechaB = b.fechaIngreso || '';

      const comparacionFecha = fechaB.localeCompare(fechaA);

      if (comparacionFecha !== 0) {
        return comparacionFecha;
      }

      return b.id - a.id;
    });
  }

  get lotesFiltrados(): Lote[] {
    const termino = this.busqueda.trim().toLocaleLowerCase();

    return this.lotesOrdenados.filter(lote => {

      const coincideBusqueda =
        !termino ||
        (lote.numeroLote || '').toLocaleLowerCase().includes(termino) ||
        (lote.nombreProducto || '').toLocaleLowerCase().includes(termino);

      const coincideEstado =
        this.filtroEstado === 'todos' ||
        (this.filtroEstado === 'activo' && lote.estado === 1) ||
        (this.filtroEstado === 'inactivo' && lote.estado === 0);

      const fecha = (lote.fechaVencimiento || '').slice(0, 10);

      let coincideVencimiento = true;

      if (this.filtroVencimiento === 'vencidos') {
        coincideVencimiento =
          lote.estado === 1 &&
          Number(lote.cantidad) > 0 &&
          fecha < this.fechaHoy;
      }

      if (this.filtroVencimiento === 'rango') {
        coincideVencimiento =
          this.rangoFechasValido &&
          lote.estado === 1 &&
          Number(lote.cantidad) > 0 &&
          fecha >= this.fechaDesde &&
          fecha <= this.fechaHasta;
      }

      return coincideBusqueda &&
        coincideEstado &&
        coincideVencimiento;
    });
  }

  // ============================================================
  // CARGAR LOTES
  // ============================================================

  cargarLotes(): void {
    this.cargandoLotes = true;
    this.errorCarga = '';

    this.loteService.listarLotes()
      .pipe(
        finalize(() => {
          this.cargandoLotes = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: respuesta => {
          this.lotes = respuesta || [];
        },

        error: (err: any) => {
          console.error('Error al cargar los lotes:', err);

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los lotes.'
          );
        }

      });
  }

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    if (err?.status === 403) {
      return err?.error?.mensaje ||
        'No tiene permisos para realizar esta operación.';
    }

    return err?.error?.mensaje ||
      err?.error?.message ||
      mensajePredeterminado;
  }

  // ============================================================
  // ABRIR Y CERRAR MODAL DE INGRESO
  // ============================================================

  abrirModalIngreso(): void {
    this.mostrarModalIngreso = true;
  }

  cerrarModalIngreso(): void {
    this.mostrarModalIngreso = false;
  }

  // ============================================================
  // ABRIR Y CERRAR MODAL DE AJUSTE
  // ============================================================

  abrirModalAjuste(lote: Lote): void {
    this.loteAjuste = lote;
    this.mostrarModalAjuste = true;
  }

  cerrarModalAjuste(): void {
    this.mostrarModalAjuste = false;
    this.loteAjuste = null;
  }

  // ============================================================
  // INDICADORES DE VENCIMIENTO
  // ============================================================

  esVencido(lote: Lote): boolean {
    return lote.estado === 1 &&
      Number(lote.cantidad) > 0 &&
      lote.fechaVencimiento < this.fechaHoy;
  }

  esPorVencer(lote: Lote): boolean {
    if (
      lote.estado !== 1 ||
      Number(lote.cantidad) <= 0 ||
      this.esVencido(lote)
    ) {
      return false;
    }

    const limite = new Date();

    limite.setDate(limite.getDate() + 30);

    const fechaLimite = this.convertirFechaAString(limite);

    return lote.fechaVencimiento <= fechaLimite;
  }

  // ============================================================
  // SOLICITAR ACTIVAR O DESACTIVAR
  // ============================================================

  solicitarCambioEstado(lote: Lote): void {
    const activar = lote.estado !== 1;

    this.confirmacionLote = {
      id: lote.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      numeroLote: lote.numeroLote,

      nombreProducto: lote.nombreProducto,

      titulo: activar
        ? 'Activar lote'
        : 'Desactivar lote',

      mensaje: activar
        ? '¿Deseas activar este lote para que vuelva a estar disponible en el inventario?'
        : '¿Deseas desactivar este lote? Se conservará su registro, pero dejará de estar disponible como lote activo.',

      textoBoton: activar
        ? 'Activar lote'
        : 'Desactivar lote'
    };

    this.errorConfirmacionLote = '';
  }

  // ============================================================
  // SOLICITAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(lote: Lote): void {
    this.confirmacionLote = {
      id: lote.id,

      tipo: 'eliminar',

      numeroLote: lote.numeroLote,

      nombreProducto: lote.nombreProducto,

      titulo: 'Eliminar lote',

      mensaje:
        '¿Estás seguro de eliminar este lote? Esta acción lo retirará de los lotes disponibles y se conservará el registro de la operación.',

      textoBoton: 'Eliminar lote'
    };

    this.errorConfirmacionLote = '';
  }

  // ============================================================
  // CERRAR CONFIRMACIÓN
  // ============================================================

  cerrarConfirmacionLote(): void {
    if (this.procesando) {
      return;
    }

    this.confirmacionLote = null;
    this.errorConfirmacionLote = '';
  }

  // ============================================================
  // EJECUTAR ACTIVAR, DESACTIVAR O ELIMINAR
  // ============================================================

  confirmarAccionLote(): void {
    const accion = this.confirmacionLote;

    if (!accion || this.procesando) {
      return;
    }

    this.procesando = true;
    this.errorConfirmacionLote = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'
        ? this.loteService.eliminarLote(accion.id)
        : this.loteService.cambiarEstado(
            accion.id,
            accion.tipo === 'activar' ? 1 : 0
          );

    peticion
      .pipe(
        finalize(() => {
          this.procesando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {
          this.confirmacionLote = null;
          this.errorConfirmacionLote = '';

          this.cargarLotes();
        },

        error: (err: any) => {
          this.errorConfirmacionLote = this.obtenerMensajeError(
            err,
            'No se pudo completar la operación. Inténtalo nuevamente.'
          );
        }

      });
  }
}


