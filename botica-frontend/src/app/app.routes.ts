import { Routes } from '@angular/router';

import { LoginComponent } from './features/auth/login/login';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';

import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { permissionGuard } from './core/guards/permission.guard';

import { ClientesListComponent } from './features/clientes/clientes-list/clientes-list';
import { CategoriasListComponent } from './features/categorias/categorias-list/categorias-list';
import { ProductosListComponent } from './features/productos/productos-list/productos-list';
import { LotesListComponent } from './features/lotes/lotes-list/lotes-list';
import { DescuentosListComponent } from './features/descuentos/descuentos-list/descuentos-list';

import { VentasPosComponent } from './features/ventas/ventas-pos/ventas-pos';
import { VentasHistorialComponent } from './features/ventas/ventas-historial/ventas-historial';

import { PerfilesListComponent } from './features/perfiles/perfiles-list/perfiles-list';
import { UsuariosListComponent } from './features/usuarios/usuarios-list/usuarios-list';

export const routes: Routes = [

  {
    path: 'login',
    component: LoginComponent
  },

  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [authGuard],

    children: [

      {
        path: 'dashboard',
        component: DashboardComponent
      },

      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'ventas',
        component: VentasPosComponent,
        canActivate: [
          permissionGuard('PUNTO_VENTA_ACCESO')
        ]
      },

      {
        path: 'historial-ventas',
        component: VentasHistorialComponent,
        canActivate: [
          permissionGuard('HISTORIAL_VENTAS_ACCESO')
        ]
      },

      {
        path: 'clientes',
        component: ClientesListComponent,
        canActivate: [
          permissionGuard('CLIENTE_ACCESO')
        ]
      },

      {
        path: 'categorias',
        component: CategoriasListComponent,
        canActivate: [
          permissionGuard('CATEGORIA_ACCESO')
        ]
      },

      {
        path: 'productos',
        component: ProductosListComponent,
        canActivate: [
          permissionGuard('PRODUCTO_ACCESO')
        ]
      },

      {
        path: 'lotes',
        component: LotesListComponent,
        canActivate: [
          permissionGuard('LOTE_ACCESO')
        ]
      },

      {
        path: 'descuentos',
        component: DescuentosListComponent,
        canActivate: [
          permissionGuard('DESCUENTO_ACCESO')
        ]
      },

      {
        path: 'perfiles',
        component: PerfilesListComponent,
        canActivate: [adminGuard]
      },

      {
        path: 'usuarios',
        component: UsuariosListComponent,
        canActivate: [adminGuard]
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];


