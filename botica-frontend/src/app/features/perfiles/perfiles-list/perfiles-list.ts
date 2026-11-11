import {
  ChangeDetectorRef,
  Component,
  OnInit,
  OnDestroy,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import { Perfil } from '../../../core/models/perfil.model';

import { PerfilService } from '../services/perfil';

import { PerfilModalComponent } from '../perfil-modal/perfil-modal';
import { PermisosModalComponent } from '../permisos-modal/permisos-modal';

type TipoConfirmacionPerfil =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionPerfil {
  id: number;
  tipo: TipoConfirmacionPerfil;
  estado?: number;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-perfiles-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PerfilModalComponent,
    PermisosModalComponent
  ],
  templateUrl: './perfiles-list.html',
  styleUrls: ['./perfiles-list.scss']
})
export class PerfilesListComponent implements OnInit, OnDestroy {

  // ============================================================
  // DATOS
  // ============================================================

  perfiles: Perfil[] = [];

  isLoading = true;

  errorCarga = '';

  mensajeNotificacion = '';

  private notificacionTimeout: ReturnType<typeof setTimeout> | null = null;

  // ============================================================
  // BÚSQUEDA Y PAGINACIÓN
  // ============================================================

  searchTerm = '';

  currentPage = 0;

  pageSize = 10;

  // ============================================================
  // MODAL DE PERFIL
  // ============================================================

  mostrarModalPerfil = false;

  perfilSeleccionado: Perfil | null = null;

  // ============================================================
  // MODAL DE PERMISOS
  // ============================================================

  perfilPermisosSeleccionado: Perfil | null = null;

  // ============================================================
  // CONFIRMACIÓN
  // ============================================================

  confirmacion: ConfirmacionPerfil | null = null;

  procesandoConfirmacion = false;

  errorConfirmacion = '';

  // ============================================================
  // SERVICIOS
  // ============================================================

  private readonly perfilService = inject(PerfilService);

  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {
    this.cargarPerfiles();
  }

  ngOnDestroy(): void {
    if (this.notificacionTimeout) {
      clearTimeout(this.notificacionTimeout);
      this.notificacionTimeout = null;
    }
  }

  mostrarNotificacion(mensaje: string): void {

    if (this.notificacionTimeout) {
      clearTimeout(this.notificacionTimeout);
    }

    this.mensajeNotificacion = mensaje;

    this.notificacionTimeout = setTimeout(() => {

      this.mensajeNotificacion = '';
      this.notificacionTimeout = null;

      this.cdr.detectChanges();

    }, 2500);
  }

  // ============================================================
  // BÚSQUEDA Y PAGINACIÓN
  // ============================================================

  get perfilesFiltrados(): Perfil[] {

    const termino = this.searchTerm.trim().toLowerCase();

    if (!termino) {
      return this.perfiles;
    }

    return this.perfiles.filter(perfil => {

      const nombre = perfil.nombre.toLowerCase();

      const descripcion = (
        perfil.descripcion ?? ''
      ).toLowerCase();

      return nombre.includes(termino)
        || descripcion.includes(termino);
    });
  }

  get totalPages(): number {

    return Math.ceil(
      this.perfilesFiltrados.length / this.pageSize
    );
  }

  get perfilesPaginados(): Perfil[] {

    const inicio = this.currentPage * this.pageSize;

    return this.perfilesFiltrados.slice(
      inicio,
      inicio + this.pageSize
    );
  }

  onSearchChange(): void {
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
  // CARGAR PERFILES
  // ============================================================

  cargarPerfiles(): void {

    this.isLoading = true;

    this.errorCarga = '';

    this.perfilService
      .listarPerfiles()
      .pipe(
        finalize(() => {

          this.isLoading = false;

          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: response => {

          this.perfiles = response ?? [];

          this.ajustarPagina();
        },

        error: err => {

          console.error(
            'Error al cargar perfiles:',
            err
          );

          this.perfiles = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los perfiles.'
          );
        }

      });
  }

  // ============================================================
  // ABRIR / CERRAR MODAL DE PERFIL
  // ============================================================

  abrirModalPerfil(perfil?: Perfil): void {

    // ADMIN es un perfil protegido.
    if (perfil?.protegido) {
      return;
    }

    this.perfilSeleccionado = perfil ?? null;

    this.mostrarModalPerfil = true;
  }

  cerrarModalPerfil(): void {

    this.mostrarModalPerfil = false;

    this.perfilSeleccionado = null;
  }

  // Se ejecuta después de guardar desde PerfilModal.
  alGuardarPerfil(): void {
    const mensaje = this.perfilSeleccionado
      ? 'Los datos del perfil se actualizaron correctamente.'
      : 'El perfil se registró correctamente.';

    this.mostrarNotificacion(mensaje);

    this.cargarPerfiles();
  }

  // ============================================================
  // ABRIR / CERRAR MODAL DE PERMISOS
  // ============================================================

  abrirModalPermisos(perfil: Perfil): void {

    if (perfil.protegido) {
      return;
    }

    this.perfilPermisosSeleccionado = perfil;
  }

  cerrarModalPermisos(): void {

    this.perfilPermisosSeleccionado = null;
  }

  // ============================================================
  // ACTUALIZAR PERFIL DESPUÉS DE GUARDAR PERMISOS
  // ============================================================

  actualizarPerfilEnLista(perfilActualizado: Perfil): void {

    this.perfiles = this.perfiles.map(perfil =>

      perfil.id === perfilActualizado.id
        ? perfilActualizado
        : perfil
    );

    this.mostrarNotificacion(
      'Los módulos del perfil se actualizaron correctamente.'
    );
  }

  // ============================================================
  // SOLICITAR ACTIVACIÓN / DESACTIVACIÓN
  // ============================================================

  solicitarCambioEstado(
    perfil: Perfil,
    nuevoEstado: number
  ): void {

    if (perfil.protegido) {
      return;
    }

    const activar = nuevoEstado === 1;

    this.confirmacion = {

      id: perfil.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      estado: nuevoEstado,

      nombre: perfil.nombre,

      titulo: activar
        ? 'Activar perfil'
        : 'Desactivar perfil',

      mensaje: activar
        ? '¿Desea activar este perfil?'
        : '¿Desea desactivar este perfil? Los usuarios asociados no podrán utilizarlo mientras permanezca inactivo.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // SOLICITAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(perfil: Perfil): void {

    if (perfil.protegido) {
      return;
    }

    this.confirmacion = {

      id: perfil.id,

      tipo: 'eliminar',

      nombre: perfil.nombre,

      titulo: 'Eliminar perfil',

      mensaje:
        '¿Está seguro de eliminar este perfil? El registro se conservará como eliminado y ya no podrá utilizarse.',

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
  // EJECUTAR ACCIÓN CONFIRMADA
  // ============================================================

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    // Comprobar si el perfil todavía tiene usuarios asociados.
    if (
      accion.tipo === 'desactivar'
      || accion.tipo === 'eliminar'
    ) {

      const perfil = this.perfiles.find(
        item => item.id === accion.id
      );

      if (perfil && perfil.totalUsuarios > 0) {

        this.errorConfirmacion =
          accion.tipo === 'desactivar'
            ? 'Primero reasigna a los usuarios vinculados para desactivar este perfil.'
            : 'Primero reasigna a los usuarios vinculados para eliminar este perfil.';

        return;
      }
    }

    this.procesandoConfirmacion = true;

    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'

        ? this.perfilService.eliminarPerfil(accion.id)

        : this.perfilService.cambiarEstado(
            accion.id,
            accion.estado ?? 0
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

          this.mostrarNotificacion(
            accion.tipo === 'eliminar'
              ? 'El perfil se eliminó correctamente.'
              : accion.tipo === 'activar'
                ? 'El perfil se activó correctamente.'
                : 'El perfil se desactivó correctamente.'
          );

          this.confirmacion = null;

          this.cargarPerfiles();
        },

        error: err => {

          this.errorConfirmacion = this.obtenerMensajeError(
            err,
            'No se pudo completar la acción.'
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

    if (err?.status === 409) {

      return mensaje
        || 'No se pudo realizar la acción porque hay un problema con el perfil.';
    }

    return mensaje || mensajePredeterminado;
  }

}


