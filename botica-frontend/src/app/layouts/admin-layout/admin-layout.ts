import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.scss']
})
export class AdminLayoutComponent {
  userRole: string = '';
  userName: string = '';
  isSidebarOpen: boolean = true;

  // Definimos los módulos del sistema
  menuItems = [
    { label: 'Inicio', route: '/dashboard', icon: '🏠', roles: ['ADMIN', 'VENDEDOR'] },
    { label: 'Punto de Venta', route: '/ventas', icon: '🛒', roles: ['ADMIN', 'VENDEDOR'] },
    { label: 'Clientes', route: '/clientes', icon: '👥', roles: ['ADMIN', 'VENDEDOR'] },
    { label: 'Productos', route: '/productos', icon: '📦', roles: ['ADMIN'] },
    { label: 'Reportes', route: '/reportes', icon: '📊', roles: ['ADMIN'] },
    { label: 'Usuarios', route: '/usuarios', icon: '⚙️', roles: ['ADMIN'] }
  ];

  constructor(private authService: AuthService, private router: Router) {
    this.userRole = this.authService.getUserRole();
    const user = localStorage.getItem('user');
    if (user) {
      const userData = JSON.parse(user);
      this.userName = `${userData.nombres} ${userData.apellidos}`;
    }
  }

  // Filtra los menús según el rol
  get allowedMenuItems() {
    return this.menuItems.filter(item => item.roles.includes(this.userRole));
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  cerrarSesion() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}


