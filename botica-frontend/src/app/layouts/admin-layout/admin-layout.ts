import {
  Component,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  RouterModule,
  Router
} from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

interface MenuItem {
  label: string;
  route: string;
  icon: string;
  roles: string[];
  permission?: string;
}

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule
  ],
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.scss']
})
export class AdminLayoutComponent {

  // ============================================================
  // SERVICIOS
  // ============================================================

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // ============================================================
  // DATOS DEL USUARIO
  // ============================================================

  userRole = '';
  userName = '';
  userPermissions: string[] = [];

  // ============================================================
  // ESTADO DE LA INTERFAZ
  // ============================================================

  isSidebarOpen = true;

  // Estado del aviso de expiración.
  sessionWarning$ = this.authService.sessionWarning$;

  // Cuenta regresiva.
  sessionSecondsRemaining$ =
    this.authService.sessionSecondsRemaining$;

  // Indica si se está esperando una operación activa.
  sessionClosing$ = this.authService.sessionClosing$;

  // ============================================================
  // ELEMENTOS DEL MENÚ
  // ============================================================

  menuItems: MenuItem[] = [

    {
      label: 'Inicio',
      route: '/dashboard',
      icon: 'bi-house-door',
      roles: ['ADMIN', 'VENDEDOR']
    },

    {
      label: 'Punto de Venta',
      route: '/ventas',
      icon: 'bi-cart3',
      roles: ['ADMIN', 'VENDEDOR']
    },

    {
      label: 'Historial de ventas',
      route: '/historial-ventas',
      icon: 'bi-receipt',
      roles: ['ADMIN', 'VENDEDOR']
    },

    {
      label: 'Clientes',
      route: '/clientes',
      icon: 'bi-people',
      roles: ['ADMIN', 'VENDEDOR']
    },

    {
      label: 'Categorías',
      route: '/categorias',
      icon: 'bi-tags',
      roles: ['ADMIN'],
      permission: 'CATEGORIA_LEER'
    },

    {
      label: 'Productos',
      route: '/productos',
      icon: 'bi-box-seam',
      roles: ['ADMIN'],
      permission: 'PRODUCTO_LEER'
    },

    {
      label: 'Lotes',
      route: '/lotes',
      icon: 'bi-boxes',
      roles: ['ADMIN', 'VENDEDOR'],
      permission: 'LOTE_LEER'
    },

    {
      label: 'Descuentos',
      route: '/descuentos',
      icon: 'bi-percent',
      roles: ['ADMIN'],
      permission: 'DESCUENTO_LEER'
    },

    {
      label: 'Reportes',
      route: '/reportes',
      icon: 'bi-bar-chart',
      roles: ['ADMIN']
    },

    {
      label: 'Usuarios',
      route: '/usuarios',
      icon: 'bi-gear',
      roles: ['ADMIN']
    }

  ];

  // ============================================================
  // CONSTRUCTOR
  // ============================================================

  constructor() {

    this.userRole = this.authService.getUserRole();

    const user = localStorage.getItem('user');

    if (user) {

      try {

        const userData = JSON.parse(user);

        this.userName =
          `${userData.nombres || ''} ${userData.apellidos || ''}`.trim();

        this.userPermissions = Array.isArray(userData.permisos)
          ? userData.permisos
          : [];

      } catch (error) {

        console.error(
          'No se pudieron leer los datos del usuario:',
          error
        );
      }
    }
  }

  // ============================================================
  // FILTRAR OPCIONES DEL MENÚ
  // ============================================================

  get allowedMenuItems(): MenuItem[] {

    return this.menuItems.filter(item => {

      if (item.permission) {

        return this.userRole === 'ADMIN' ||
          this.userPermissions.includes(item.permission);
      }

      return item.roles.includes(this.userRole);
    });
  }

  // ============================================================
  // SIDEBAR
  // ============================================================

  toggleSidebar(): void {

    this.isSidebarOpen = !this.isSidebarOpen;
  }

  // ============================================================
  // CERRAR SESIÓN VOLUNTARIAMENTE
  // ============================================================

  cerrarSesion(): void {

    this.authService.logout();

    void this.router.navigate(['/login']);
  }

  // ============================================================
  // FORMATEAR CUENTA REGRESIVA
  // ============================================================

  formatearTiempoSesion(segundos: number | null): string {

    const total = Math.max(0, segundos ?? 0);

    const minutos = Math.floor(total / 60);

    const restantes = total % 60;

    return `${String(minutos).padStart(2, '0')}:${String(restantes).padStart(2, '0')}`;
  }

  // ============================================================
  // OCULTAR AVISO
  // ============================================================

  ocultarAvisoSesion(): void {

    this.authService.ocultarAvisoSesion();
  }

}


