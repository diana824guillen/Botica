import {
  ChangeDetectorRef,
  Component,
  OnInit,
  OnDestroy,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  Observable,
  Subject,
  Subscription,
  debounceTime,
  distinctUntilChanged,
  finalize
} from 'rxjs';

import { Usuario } from '../../../core/models/usuario.model';

import { UsuarioService } from '../services/usuario';

import { UsuarioModalComponent } from '../usuario-modal/usuario-modal';
import { UsuarioClaveModalComponent } from '../usuario-clave-modal/usuario-clave-modal';

type TipoConfirmacionUsuario =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionUsuario {
  id: number;
  tipo: TipoConfirmacionUsuario;
  estado?: number;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-usuarios-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    UsuarioModalComponent,
    UsuarioClaveModalComponent
  ],
  templateUrl: './usuarios-list.html',
  styleUrls: ['./usuarios-list.scss']
})
export class UsuariosListComponent implements OnInit, OnDestroy {

  // ============================================================
  // DATOS
  // ============================================================

  usuarios: Usuario[] = [];

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

  totalElements = 0;

  totalPages = 0;

  private readonly searchSubject = new Subject<string>();

  private searchSubscription: Subscription | null = null;

  // ============================================================
  // MODAL DE REGISTRO Y EDICIÓN
  // ============================================================

  mostrarModalUsuario = false;

  usuarioSeleccionado: Usuario | null = null;

  // ============================================================
  // MODAL DE RESTABLECIMIENTO DE CONTRASEÑA
  // ============================================================

  mostrarModalClave = false;

  usuarioClaveSeleccionado: Usuario | null = null;

  // ============================================================
  // CONFIRMACIÓN
  // ============================================================

  confirmacion: ConfirmacionUsuario | null = null;

  procesandoConfirmacion = false;

  errorConfirmacion = '';

  // ============================================================
  // SERVICIOS
  // ============================================================

  private readonly usuarioService = inject(UsuarioService);

  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {

    this.searchSubscription = this.searchSubject
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe(() => {

        this.currentPage = 0;

        this.cargarUsuarios();
      });

    this.cargarUsuarios();
  }

  ngOnDestroy(): void {

    this.searchSubscription?.unsubscribe();

    this.searchSubject.complete();

    if (this.notificacionTimeout) {
      clearTimeout(this.notificacionTimeout);
      this.notificacionTimeout = null;
    }
  }

  // ============================================================
  // NOTIFICACIONES
  // ============================================================

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
  // CARGAR USUARIOS
  // ============================================================

  cargarUsuarios(): void {

    this.isLoading = true;

    this.errorCarga = '';

    this.usuarioService
      .listarUsuarios(
        this.currentPage,
        this.pageSize,
        this.searchTerm
      )
      .pipe(
        finalize(() => {

          this.isLoading = false;

          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: response => {

          this.usuarios = response.content ?? [];

          this.totalElements = response.totalElements ?? 0;

          this.totalPages = response.totalPages ?? 0;

          if (this.totalPages === 0) {
            this.currentPage = 0;
          }
        },

        error: err => {

          console.error(
            'Error al cargar usuarios:',
            err
          );

          this.usuarios = [];

          this.totalElements = 0;

          this.totalPages = 0;

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los usuarios.'
          );
        }

      });
  }

  // ============================================================
  // BÚSQUEDA
  // ============================================================

  onSearchInput(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.searchTerm = input.value;

    this.searchSubject.next(input.value);
  }

  // ============================================================
  // PAGINACIÓN
  // ============================================================

  cambiarPagina(page: number): void {

    if (page >= 0 && page < this.totalPages) {

      this.currentPage = page;

      this.cargarUsuarios();
    }
  }

  // ============================================================
  // ABRIR / CERRAR MODAL DE USUARIO
  // ============================================================

  abrirModalUsuario(usuario?: Usuario): void {

    if (usuario?.protegido) {
      return;
    }

    this.usuarioSeleccionado = usuario ?? null;

    this.mostrarModalUsuario = true;
  }

  cerrarModalUsuario(): void {

    this.mostrarModalUsuario = false;

    this.usuarioSeleccionado = null;
  }

  alGuardarUsuario(): void {

    const mensaje = this.usuarioSeleccionado
      ? 'Los datos del usuario se actualizaron correctamente.'
      : 'El usuario se registró correctamente.';

    this.mostrarNotificacion(mensaje);

    this.cargarUsuarios();
  }

  // ============================================================
  // ABRIR / CERRAR MODAL DE CONTRASEÑA
  // ============================================================

  abrirModalClave(usuario: Usuario): void {

    if (usuario.protegido) {
      return;
    }

    this.usuarioClaveSeleccionado = usuario;

    this.mostrarModalClave = true;
  }

  cerrarModalClave(): void {

    this.mostrarModalClave = false;

    this.usuarioClaveSeleccionado = null;
  }

  alRestablecerClave(): void {

    this.mostrarNotificacion(
      'La contraseña se restableció correctamente.'
    );

    // Actualiza el estado de bloqueo del usuario.
    this.cargarUsuarios();
  }

  // ============================================================
  // SOLICITAR ACTIVACIÓN / DESACTIVACIÓN
  // ============================================================

  solicitarCambioEstado(
    usuario: Usuario,
    nuevoEstado: number
  ): void {

    if (usuario.protegido) {
      return;
    }

    const activar = nuevoEstado === 1;

    const nombre =
      `${usuario.nombres} ${usuario.apellidos}`.trim();

    this.confirmacion = {

      id: usuario.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      estado: nuevoEstado,

      nombre,

      titulo: activar
        ? 'Activar usuario'
        : 'Desactivar usuario',

      mensaje: activar
        ? '¿Desea activar este usuario? Podrá volver a iniciar sesión en el sistema.'
        : '¿Desea desactivar este usuario? Ya no podrá iniciar sesión mientras permanezca inactivo.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // SOLICITAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(usuario: Usuario): void {

    if (usuario.protegido) {
      return;
    }

    const nombre =
      `${usuario.nombres} ${usuario.apellidos}`.trim();

    this.confirmacion = {

      id: usuario.id,

      tipo: 'eliminar',

      nombre,

      titulo: 'Eliminar usuario',

      mensaje:
        '¿Está seguro de que desea eliminar este usuario? El registro se conservará como eliminado y ya no podrá iniciar sesión.',

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

    this.procesandoConfirmacion = true;

    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'

        ? this.usuarioService.eliminarUsuario(accion.id)

        : this.usuarioService.cambiarEstado(
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

          // Si se elimina el último registro de la página,
          // retrocede para evitar mostrar una página vacía.
          if (
            accion.tipo === 'eliminar'
            && this.usuarios.length === 1
            && this.currentPage > 0
          ) {
            this.currentPage--;
          }

          this.mostrarNotificacion(
            accion.tipo === 'eliminar'
              ? 'El usuario se eliminó correctamente.'
              : accion.tipo === 'activar'
                ? 'El usuario se activó correctamente.'
                : 'El usuario se desactivó correctamente.'
          );

          this.confirmacion = null;

          this.cargarUsuarios();
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
  // OBTENER MENSAJE DE ERROR
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
        || 'No se pudo realizar la acción porque existe un conflicto.';
    }

    return mensaje || mensajePredeterminado;
  }

}


