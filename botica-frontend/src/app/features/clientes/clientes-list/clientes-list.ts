import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../services/cliente';
import { HasPermissionDirective } from '../../../core/directives/has-permission';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ClienteModalComponent } from '../cliente-modal/cliente-modal';

@Component({
  selector: 'app-clientes-list',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective, ClienteModalComponent],
  templateUrl: './clientes-list.html',
  styleUrls: ['./clientes-list.scss']
})
export class ClientesListComponent implements OnInit {
  clientes: any[] = [];
  isLoading: boolean = true;
  mostrarModal: boolean = false;
  clienteSeleccionado: any = null; // Para edición

  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;

  searchTerm: string = '';
  private searchSubject = new Subject<string>();

  private clienteService = inject(ClienteService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    // Configurar búsqueda con retardo (debounce)
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(search => {
      this.searchTerm = search;
      this.currentPage = 0;
      this.cargarClientes();
    });

    this.cargarClientes();
  }

  cargarClientes() {
    this.isLoading = true;
    this.clienteService.listarClientes(this.currentPage, this.pageSize, this.searchTerm).subscribe({
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

  onSearchInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.searchSubject.next(input.value);
  }

  cambiarPagina(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.cargarClientes();
    }
  }

  cambiarEstado(id: number, nuevoEstado: number) {
    const accion = nuevoEstado === 1 ? 'activar' : 'desactivar';
    if (confirm(`¿Estás seguro de ${accion} este cliente?`)) {
      this.clienteService.cambiarEstado(id, nuevoEstado).subscribe({
        next: () => this.cargarClientes(),
        error: (err) => {
          console.error('Error al cambiar estado', err);
          alert('Ocurrió un error al cambiar el estado del cliente.');
        }
      });
    }
  }

  abrirModal(cliente?: any) {
    this.clienteSeleccionado = cliente || null;
    this.mostrarModal = true;
  }

  cerrarModal() {
    this.mostrarModal = false;
    this.clienteSeleccionado = null;
  }
}


