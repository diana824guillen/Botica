import { Routes } from '@angular/router';

import { LoginComponent } from './features/auth/login/login';
import { DashboardComponent } from './features/dashboard/dashboard/dashboard';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';

import { authGuard } from './core/guards/auth.guard';

import { ClientesListComponent } from './features/clientes/clientes-list/clientes-list';
import { CategoriasListComponent } from './features/categorias/categorias-list/categorias-list';

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
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'login'
  }

];


