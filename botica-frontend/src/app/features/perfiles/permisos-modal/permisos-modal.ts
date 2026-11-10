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

interface GrupoOpciones {
  modulo: string;
  opciones: OpcionPermiso[];
}

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
  // DATOS
  // ============================================================

  opciones: OpcionPermiso[] = [];

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
  // CARGAR OPCIONES DISPONIBLES
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

          this.opciones = response ?? [];

          this.normalizarPermisosSeleccionados();
        },

        error: err => {

          console.error(
            'Error al cargar los permisos:',
            err
          );

          this.opciones = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los permisos disponibles.'
          );
        }

      });
  }

  // ============================================================
  // AGRUPAR OPCIONES POR MÓDULO
  // ============================================================

  get gruposOpciones(): GrupoOpciones[] {

    const grupos = new Map<string, OpcionPermiso[]>();

    for (const opcion of this.opciones) {

      const modulo = opcion.modulo || 'Otros';

      if (!grupos.has(modulo)) {
        grupos.set(modulo, []);
      }

      grupos.get(modulo)?.push(opcion);
    }

    const resultado: GrupoOpciones[] = [];

    grupos.forEach((opciones, modulo) => {

      resultado.push({

        modulo,

        opciones: [...opciones].sort((a, b) => {

          const ordenA = this.obtenerOrdenAccion(a.accion);

          const ordenB = this.obtenerOrdenAccion(b.accion);

          if (ordenA !== ordenB) {
            return ordenA - ordenB;
          }

          return a.accion.localeCompare(b.accion);
        })

      });

    });

    return resultado;
  }

  private obtenerOrdenAccion(accion: string): number {

    const orden = [
      'Consultar',
      'Previsualizar',
      'Registrar',
      'Editar',
      'Cambiar estado',
      'Ajustar',
      'Ver detalle',
      'Anular',
      'Eliminar',
      'Descargar',
      'Imprimir',
      'Exportar PDF',
      'Exportar Excel',
      'Restablecer contraseña'
    ];

    const indice = orden.indexOf(accion);

    return indice === -1 ? orden.length : indice;
  }

  // ============================================================
  // IDENTIFICAR PERMISO DE CONSULTA
  // ============================================================

  esPermisoConsulta(opcion: OpcionPermiso): boolean {

    return opcion.accion.trim().toLowerCase() === 'consultar';
  }

  // ============================================================
  // NORMALIZAR SELECCIÓN INICIAL
  // ============================================================

  private normalizarPermisosSeleccionados(): void {

    for (const opcion of this.opciones) {

      if (
        this.esPermisoConsulta(opcion)
        || !this.permisosSeleccionados.has(opcion.id)
      ) {
        continue;
      }

      const permisoConsulta = this.opciones.find(item =>
        item.modulo === opcion.modulo
        && this.esPermisoConsulta(item)
      );

      if (permisoConsulta) {

        this.permisosSeleccionados.add(
          permisoConsulta.id
        );
      }
    }
  }

  // ============================================================
  // VERIFICAR SI UNA OPCIÓN ESTÁ SELECCIONADA
  // ============================================================

  estaSeleccionado(opcion: OpcionPermiso): boolean {

    return this.permisosSeleccionados.has(opcion.id);
  }

  // ============================================================
  // VERIFICAR ACCIONES SELECCIONADAS EN UN MÓDULO
  // ============================================================

  tieneAccionesSeleccionadas(modulo: string): boolean {

    return this.opciones.some(opcion =>

      opcion.modulo === modulo
      && !this.esPermisoConsulta(opcion)
      && this.permisosSeleccionados.has(opcion.id)

    );
  }

  // ============================================================
  // VERIFICAR SI TODO EL MÓDULO ESTÁ SELECCIONADO
  // ============================================================

  moduloSeleccionado(modulo: string): boolean {

    const opcionesModulo = this.opciones.filter(
      opcion => opcion.modulo === modulo
    );

    return opcionesModulo.length > 0
      && opcionesModulo.every(opcion =>
          this.permisosSeleccionados.has(opcion.id)
      );
  }

  // ============================================================
  // SELECCIONAR / DESELECCIONAR TODO UN MÓDULO
  // ============================================================

  alternarModulo(
    modulo: string,
    seleccionado: boolean
  ): void {

    if (this.isSaving) {
      return;
    }

    const opcionesModulo = this.opciones.filter(
      opcion => opcion.modulo === modulo
    );

    for (const opcion of opcionesModulo) {

      if (seleccionado) {
        this.permisosSeleccionados.add(opcion.id);
      } else {
        this.permisosSeleccionados.delete(opcion.id);
      }
    }
  }

  // ============================================================
  // SELECCIONAR / DESELECCIONAR PERMISOS
  // ============================================================

  alternarPermiso(
    opcion: OpcionPermiso,
    seleccionado: boolean
  ): void {

    if (this.isSaving) {
      return;
    }

    // Consultar no se puede retirar mientras existan
    // otras acciones seleccionadas del mismo módulo.
    if (
      this.esPermisoConsulta(opcion)
      && !seleccionado
      && this.tieneAccionesSeleccionadas(opcion.modulo)
    ) {
      return;
    }

    if (seleccionado) {

      this.permisosSeleccionados.add(opcion.id);

      // Al seleccionar otra acción, agregar Consultar.
      if (!this.esPermisoConsulta(opcion)) {

        const permisoConsulta = this.opciones.find(item =>
          item.modulo === opcion.modulo
          && this.esPermisoConsulta(item)
        );

        if (permisoConsulta) {

          this.permisosSeleccionados.add(
            permisoConsulta.id
          );
        }
      }

    } else {

      this.permisosSeleccionados.delete(opcion.id);
    }
  }

  // ============================================================
  // TOTAL DE PERMISOS SELECCIONADOS
  // ============================================================

  get totalPermisosSeleccionados(): number {

    return this.permisosSeleccionados.size;
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
  // GUARDAR PERMISOS
  // ============================================================

  guardarPermisos(): void {

    if (this.isLoading || this.isSaving || this.errorCarga) {
      return;
    }

    const opcionIds = Array.from(
      this.permisosSeleccionados
    ).sort((a, b) => a - b);

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
            'Error al actualizar los permisos:',
            err
          );

          this.errorMessage = this.obtenerMensajeError(
            err,
            'No se pudieron actualizar los permisos del perfil.'
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


