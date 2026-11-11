
import {
  Component,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent {

  private readonly authService = inject(AuthService);

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


