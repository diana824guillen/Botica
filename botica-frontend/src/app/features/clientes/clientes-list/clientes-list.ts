import {
  Component,
  OnInit,
  ChangeDetectorRef,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Subject, debounceTime, distinctUntilChanged, Observable } from 'rxjs';

import { ClienteService } from '../services/cliente';
import { HasPermissionDirective } from '../../../core/directives/has-permission';
import { ClienteModalComponent } from '../cliente-modal/cliente-modal';

type TipoConfirmacionCliente =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionCliente {
  id: number;
  tipo: TipoConfirmacionCliente;
  nombre: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-clientes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective,
    ClienteModalComponent
  ],
  templateUrl: './clientes-list.html',
  styleUrls: ['./clientes-list.scss']
})
export class ClientesListComponent implements OnInit {

  clientes: any[] = [];

  isLoading = true;

  mostrarModal = false;
  clienteSeleccionado: any = null;

  currentPage = 0;
  pageSize = 10;

  totalElements = 0;
  totalPages = 0;

  searchTerm = '';

  // Datos del modal de confirmación.
  confirmacion: ConfirmacionCliente | null = null;

  procesandoConfirmacion = false;
  errorConfirmacion = '';

  private searchSubject = new Subject<string>();

  private clienteService = inject(ClienteService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {

    // Evita consultar el servidor por cada tecla escrita.
    this.searchSubject
      .pipe(
        debounceTime(400),
        distinctUntilChanged()
      )
      .subscribe(search => {

        this.searchTerm = search;
        this.currentPage = 0;

        this.cargarClientes();
      });

    this.cargarClientes();
  }

  cargarClientes(): void {

    this.isLoading = true;

    this.clienteService
      .listarClientes(
        this.currentPage,
        this.pageSize,
        this.searchTerm
      )
      .subscribe({

        next: (response: any) => {

          this.clientes = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;

          this.isLoading = false;

          this.cdr.detectChanges();
        },

        error: (err: any) => {

          console.error('Error al cargar clientes', err);

          this.isLoading = false;

          this.cdr.detectChanges();
        }
      });
  }

  onSearchInput(event: Event): void {

    const input = event.target as HTMLInputElement;

    this.searchSubject.next(input.value);
  }

  cambiarPagina(page: number): void {

    if (page >= 0 && page < this.totalPages) {

      this.currentPage = page;

      this.cargarClientes();
    }
  }

  // ============================================================
  // SOLICITAR ACTIVACIÓN O DESACTIVACIÓN
  // ============================================================

  solicitarCambioEstado(
    cliente: any,
    nuevoEstado: number
  ): void {

    const activar = nuevoEstado === 1;

    const nombre =
      `${cliente.nombres} ${cliente.apellidos || ''}`.trim();

    this.confirmacion = {

      id: cliente.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      nombre,

      titulo: activar
        ? 'Activar cliente'
        : 'Desactivar cliente',

      mensaje: activar
        ? '¿Desea activar este cliente? Podrá volver a utilizarse en las operaciones del sistema.'
        : '¿Desea desactivar este cliente? El registro se conservará, pero quedará inactivo.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // SOLICITAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(cliente: any): void {

    const nombre =
      `${cliente.nombres} ${cliente.apellidos || ''}`.trim();

    this.confirmacion = {

      id: cliente.id,

      tipo: 'eliminar',

      nombre,

      titulo: 'Eliminar cliente',

      mensaje:
        '¿Está seguro de que desea eliminar este cliente?',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // CERRAR MODAL DE CONFIRMACIÓN
  // ============================================================

  cerrarConfirmacion(): void {

    // No cerrar mientras se procesa una operación.
    if (this.procesandoConfirmacion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  // ============================================================
  // EJECUTAR LA ACCIÓN CONFIRMADA
  // ============================================================

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    this.procesandoConfirmacion = true;
    this.errorConfirmacion = '';

    // Elegir la petición según la acción confirmada.
    const peticion: Observable<unknown> = accion.tipo === 'eliminar'
      ? this.clienteService.eliminarCliente(accion.id)
      : this.clienteService.cambiarEstado(accion.id, accion.tipo === 'activar' ? 1 : 0);

    peticion.subscribe({

      next: () => {

        this.procesandoConfirmacion = false;

        this.confirmacion = null;

        // Actualizar la tabla después de completar la operación.
        this.cargarClientes();
      },

      error: (err: any) => {

        this.procesandoConfirmacion = false;

        this.errorConfirmacion =
          err?.error?.mensaje
          || 'No se pudo completar la acción. Inténtelo nuevamente.';
      }
    });
  }

  // ============================================================
  // ABRIR Y CERRAR FORMULARIO DE CLIENTES
  // ============================================================

  abrirModal(cliente?: any): void {

    this.clienteSeleccionado = cliente || null;
    this.mostrarModal = true;
  }

  cerrarModal(): void {

    this.mostrarModal = false;
    this.clienteSeleccionado = null;
  }
}


