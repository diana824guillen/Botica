import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

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
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.scss']
})
export class AdminLayoutComponent {

  userRole = '';
  userName = '';
  userPermissions: string[] = [];

  isSidebarOpen = true;

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

  constructor(
    private authService: AuthService,
    private router: Router
  ) {

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
        console.error('No se pudieron leer los datos del usuario:', error);
      }
    }
  }

  get allowedMenuItems(): MenuItem[] {

    return this.menuItems.filter(item => {

      // El menú de Categorías depende del permiso de lectura.
      if (item.permission) {
        return this.userRole === 'ADMIN' ||
          this.userPermissions.includes(item.permission);
      }

      // Los demás menús mantienen el control actual por rol.
      return item.roles.includes(this.userRole);
    });
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}


