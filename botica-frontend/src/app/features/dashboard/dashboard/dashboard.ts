
import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { finalize } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

import {
  Producto,
  Presentacion,
  TipoPresentacion
} from '../../../core/models/producto.model';

import { ProductoService } from '../../productos/services/producto';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly productoService = inject(ProductoService);
  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // ALERTAS DE STOCK
  // ============================================================

  productosStockBajo: Producto[] = [];

  cargandoAlertasStock = false;
  errorAlertasStock = '';

  ngOnInit(): void {

    if (this.puedeConsultarAlertasStock) {
      this.cargarAlertasStockBajo();
    }
  }

  get puedeConsultarAlertasStock(): boolean {

    return this.authService.hasPermission('PRODUCTO_ACCESO')
      || this.authService.hasPermission('LOTE_ACCESO');
  }

  cargarAlertasStockBajo(): void {

    this.cargandoAlertasStock = true;
    this.errorAlertasStock = '';

    this.productoService
      .listarProductosConStockBajo()
      .pipe(
        finalize(() => {
          this.cargandoAlertasStock = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.productosStockBajo = respuesta || [];
        },

        error: (err) => {

          console.error(
            'Error al cargar alertas de stock:',
            err
          );

          if (err?.status === 403) {

            this.errorAlertasStock =
              err?.error?.mensaje ||
              'No tiene permisos para consultar las alertas de inventario.';

          } else if (err?.status === 0) {

            this.errorAlertasStock =
              'No se pudo conectar con el servidor.';

          } else {

            this.errorAlertasStock =
              err?.error?.mensaje ||
              err?.error?.message ||
              'No se pudieron cargar las alertas de stock.';
          }
        }

      });
  }

  presentacionesConStockBajo(
    producto: Producto
  ): Presentacion[] {

    return (producto.presentaciones || []).filter(p => {

      const stockMinimo = Number(p.stockMinimo ?? 0);
      const stockDisponible = Number(p.stockDisponible ?? 0);

      return p.estado === 1
        && stockMinimo > 0
        && stockDisponible <= stockMinimo;
    });
  }

  get totalAlertasStockBajo(): number {

    return this.productosStockBajo.reduce(
      (total, producto) =>
        total + this.presentacionesConStockBajo(producto).length,
      0
    );
  }

  etiquetaTipo(tipo: TipoPresentacion): string {

    const nombres: Record<TipoPresentacion, string> = {
      UNIDAD: 'Unidad',
      BLISTER: 'Blíster',
      CAJA: 'Caja',
      FRASCO: 'Frasco',
      TIRA: 'Tira',
      OTRO: 'Otro'
    };

    return nombres[tipo] || tipo;
  }

  // ============================================================
  // ACCESO A MÓDULOS
  // ============================================================

  get puedeVerProductos(): boolean {

    return this.authService.hasPermission(
      'PRODUCTO_ACCESO'
    );
  }

  get puedeRegistrarVenta(): boolean {

    return this.authService.hasPermission(
      'PUNTO_VENTA_ACCESO'
    );
  }

  get puedeVerClientes(): boolean {

    return this.authService.hasPermission(
      'CLIENTE_ACCESO'
    );
  }

  get mostrarTarjetas(): boolean {

    return this.puedeVerProductos
      || this.puedeRegistrarVenta
      || this.puedeVerClientes;
  }
}


