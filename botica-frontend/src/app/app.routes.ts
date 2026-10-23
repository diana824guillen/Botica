import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { 
    path: 'dashboard', 
    // canActivate: [authGuard], // Lo activaremos cuando creemos el Dashboard
    loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent) // Temporal para que no falle
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];


