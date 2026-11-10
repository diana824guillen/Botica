import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs';

import {
  Perfil,
  OpcionPermiso
} from '../../../core/models/perfil.model';

import { PerfilService } from '../services/perfil';

const ORDEN_MODULOS = [
  'CLIENTE_ACCESO',
  'CATEGORIA_ACCESO',
  'PRODUCTO_ACCESO',
  'LOTE_ACCESO',
  'PUNTO_VENTA_ACCESO',
  'HISTORIAL_VENTAS_ACCESO',
  'DESCUENTO_ACCESO'
];

const NOMBRES_MODULOS: Record<string, string> = {
  CLIENTE_ACCESO: 'Clientes',
  CATEGORIA_ACCESO: 'Categorías',
  PRODUCTO_ACCESO: 'Productos',
  LOTE_ACCESO: 'Lotes e inventario',
  PUNTO_VENTA_ACCESO: 'Punto de venta',
  HISTORIAL_VENTAS_ACCESO: 'Historial de ventas',
  DESCUENTO_ACCESO: 'Descuentos'
};

@Component({
  selector: 'app-permisos-modal',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './permisos-modal.html',
  styleUrls: ['./permisos-modal.scss']
})
export class PermisosModalComponent implements OnInit {

  @Input({ required: true }) perfil!: Perfil;

  @Output() cerrar = new EventEmitter<void>();

  @Output() guardado = new EventEmitter<Perfil>();

  // ============================================================
  // MÓDULOS DISPONIBLES
  // ============================================================

  modulosDisponibles: OpcionPermiso[] = [];

  permisosSeleccionados = new Set<number>();

  // ============================================================
  // ESTADOS
  // ============================================================

  isLoading = true;

  isSaving = false;

  errorCarga = '';

  errorMessage = '';

  // ============================================================
  // SERVICIOS
  // ============================================================

  private readonly perfilService = inject(PerfilService);

  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {

    this.permisosSeleccionados = new Set(
      this.perfil.opcionIds ?? []
    );

    this.cargarOpciones();
  }

  // ============================================================
  // CARGAR MÓDULOS DISPONIBLES
  // ============================================================

  cargarOpciones(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.perfilService
      .listarOpciones()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: response => {

          this.modulosDisponibles = (response ?? [])
            .filter(opcion =>
              opcion.estado === 1
              && ORDEN_MODULOS.includes(opcion.nombre)
            )
            .sort((a, b) =>
              ORDEN_MODULOS.indexOf(a.nombre)
              - ORDEN_MODULOS.indexOf(b.nombre)
            );

          // Conservar únicamente las asignaciones que corresponden a módulos disponibles.
          const idsDisponibles = new Set(
            this.modulosDisponibles.map(opcion => opcion.id)
          );

          this.permisosSeleccionados = new Set(
            [...this.permisosSeleccionados].filter(id =>
              idsDisponibles.has(id)
            )
          );
        },

        error: err => {

          console.error(
            'Error al cargar los módulos:',
            err
          );

          this.modulosDisponibles = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los módulos disponibles.'
          );
        }

      });
  }

  // ============================================================
  // OBTENER NOMBRE VISUAL DEL MÓDULO
  // ============================================================

  obtenerNombreModulo(opcion: OpcionPermiso): string {

    return NOMBRES_MODULOS[opcion.nombre]
      || opcion.modulo
      || 'Módulo';
  }

  // ============================================================
  // VERIFICAR SI UN MÓDULO ESTÁ HABILITADO
  // ============================================================

  estaSeleccionado(opcion: OpcionPermiso): boolean {

    return this.permisosSeleccionados.has(opcion.id);
  }

  // ============================================================
  // ACTIVAR / DESACTIVAR MÓDULO
  // ============================================================

  alternarModulo(
    opcion: OpcionPermiso,
    seleccionado: boolean
  ): void {

    if (this.isSaving) {
      return;
    }

    if (seleccionado) {

      this.permisosSeleccionados.add(opcion.id);

    } else {

      this.permisosSeleccionados.delete(opcion.id);
    }
  }

  // ============================================================
  // TOTAL DE MÓDULOS HABILITADOS
  // ============================================================

  get totalModulosSeleccionados(): number {

    return this.modulosDisponibles.filter(opcion =>
      this.permisosSeleccionados.has(opcion.id)
    ).length;
  }

  // ============================================================
  // CERRAR MODAL
  // ============================================================

  cerrarModal(): void {

    if (this.isSaving) {
      return;
    }

    this.cerrar.emit();
  }

  // ============================================================
  // GUARDAR MÓDULOS
  // ============================================================

  guardarPermisos(): void {

    if (
      this.isLoading
      || this.isSaving
      || this.errorCarga
    ) {
      return;
    }

    const opcionIds = this.modulosDisponibles
      .filter(opcion =>
        this.permisosSeleccionados.has(opcion.id)
      )
      .map(opcion => opcion.id)
      .sort((a, b) => a - b);

    this.errorMessage = '';
    this.isSaving = true;

    this.perfilService
      .actualizarPermisos(
        this.perfil.id,
        { opcionIds }
      )
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: perfilActualizado => {

          this.guardado.emit(perfilActualizado);

          this.cerrar.emit();
        },

        error: err => {

          console.error(
            'Error al actualizar los módulos:',
            err
          );

          this.errorMessage = this.obtenerMensajeError(
            err,
            'No se pudieron actualizar los módulos del perfil.'
          );
        }

      });
  }

  // ============================================================
  // MENSAJES DE ERROR
  // ============================================================

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

}


