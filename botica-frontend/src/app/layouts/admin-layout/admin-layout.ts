import {
  Component,
  OnDestroy,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  NavigationEnd,
  RouterModule,
  Router
} from '@angular/router';

import { filter, Subscription } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';

interface MenuItem {
  label: string;
  route: string;
  icon: string;
  roles?: string[];
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
export class AdminLayoutComponent implements OnDestroy {

  // ============================================================
  // SERVICIOS
  // ============================================================

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private permisosSubscription: Subscription | null = null;

  private navegacionSubscription: Subscription | null = null;

  // ============================================================
  // DATOS DEL USUARIO
  // ============================================================

  userRole = '';
  userName = '';

  // ============================================================
  // ESTADO DE LA INTERFAZ
  // ============================================================

  isSidebarOpen = true;

  sessionWarning$ = this.authService.sessionWarning$;

  sessionSecondsRemaining$ =
    this.authService.sessionSecondsRemaining$;

  sessionClosing$ =
    this.authService.sessionClosing$;

  // ============================================================
  // ELEMENTOS DEL MENÚ
  // ============================================================

  menuItems: MenuItem[] = [

    {
      label: 'Inicio',
      route: '/dashboard',
      icon: 'bi-house-door'
    },

    {
      label: 'Punto de Venta',
      route: '/ventas',
      icon: 'bi-cart3',
      permission: 'PUNTO_VENTA_ACCESO'
    },

    {
      label: 'Historial de ventas',
      route: '/historial-ventas',
      icon: 'bi-receipt',
      permission: 'HISTORIAL_VENTAS_ACCESO'
    },

    {
      label: 'Clientes',
      route: '/clientes',
      icon: 'bi-people',
      permission: 'CLIENTE_ACCESO'
    },

    {
      label: 'Categorías',
      route: '/categorias',
      icon: 'bi-tags',
      permission: 'CATEGORIA_ACCESO'
    },

    {
      label: 'Productos',
      route: '/productos',
      icon: 'bi-box-seam',
      permission: 'PRODUCTO_ACCESO'
    },

    {
      label: 'Lotes',
      route: '/lotes',
      icon: 'bi-boxes',
      permission: 'LOTE_ACCESO'
    },

    {
      label: 'Descuentos',
      route: '/descuentos',
      icon: 'bi-percent',
      permission: 'DESCUENTO_ACCESO'
    },

    {
      label: 'Perfiles y permisos',
      route: '/perfiles',
      icon: 'bi-shield-lock',
      roles: ['ADMIN']
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

      } catch (error) {

        console.error(
          'No se pudieron leer los datos del usuario:',
          error
        );
      }
    }

    // Escuchar los cambios de permisos.
    this.permisosSubscription =
      this.authService.permissions$.subscribe(() => {

        this.verificarAccesoRutaActual();
      });

    // Comprobar nuevamente la ruta cuando termina una navegación.
    this.navegacionSubscription =
      this.router.events
        .pipe(
          filter(event => event instanceof NavigationEnd)
        )
        .subscribe(() => {

          // Esperar a que finalice la navegación actual.
          setTimeout(() => {
            this.verificarAccesoRutaActual();
          }, 0);
        });
  }

  // ============================================================
  // DESTRUIR SUSCRIPCIONES
  // ============================================================

  ngOnDestroy(): void {

    this.permisosSubscription?.unsubscribe();

    this.navegacionSubscription?.unsubscribe();

    this.permisosSubscription = null;

    this.navegacionSubscription = null;
  }

  // ============================================================
  // FILTRAR OPCIONES DEL MENÚ
  // ============================================================

  get allowedMenuItems(): MenuItem[] {

    return this.menuItems.filter(item => {

      // Los módulos requieren su permiso correspondiente.
      if (item.permission) {

        return this.authService.hasPermission(
          item.permission
        );
      }

      // Las opciones administrativas dependen del rol.
      if (item.roles) {

        return item.roles.includes(this.userRole);
      }

      // Inicio está disponible para cualquier usuario
      // que haya iniciado sesión.
      return true;
    });
  }

  // ============================================================
  // VERIFICAR ACCESO A LA RUTA ACTUAL
  // ============================================================

  private verificarAccesoRutaActual(): void {

    // No comprobar mientras existe una navegación en curso.
    if (this.router.getCurrentNavigation()) {
      return;
    }

    const rutaActual = this.router.url
      .split('?')[0]
      .split('#')[0];

    const moduloActual = this.menuItems.find(item => {

      if (!item.permission) {
        return false;
      }

      return (
        rutaActual === item.route ||
        rutaActual.startsWith(`${item.route}/`)
      );
    });

    if (
      moduloActual?.permission &&
      !this.authService.hasPermission(
        moduloActual.permission
      )
    ) {

      // El usuario perdió el acceso al módulo actual.
      void this.router.navigate(['/dashboard']);
    }
  }

  // ============================================================
  // SIDEBAR
  // ============================================================

  toggleSidebar(): void {

    this.isSidebarOpen = !this.isSidebarOpen;
  }

  // ============================================================
  // CERRAR SESIÓN
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


