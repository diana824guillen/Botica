import { Routes } from '@angular/router';

import { LoginComponent } from './features/auth/login/login';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';

import { authGuard } from './core/guards/auth.guard';

import { ClientesListComponent } from './features/clientes/clientes-list/clientes-list';
import { CategoriasListComponent } from './features/categorias/categorias-list/categorias-list';
import { ProductosListComponent } from './features/productos/productos-list/productos-list';
import { LotesListComponent } from './features/lotes/lotes-list/lotes-list';

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
        path: 'clientes',
        component: ClientesListComponent
      },

      {
        path: 'categorias',
        component: CategoriasListComponent
      },

      {
        path: 'lotes',
        component: LotesListComponent
      },

      {
        path: 'productos',
        component: ProductosListComponent
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];


