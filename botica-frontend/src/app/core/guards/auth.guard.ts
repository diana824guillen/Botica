import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const authGuard = () => {

  const authService = inject(AuthService);
  const router = inject(Router);

  // Comprobar si existía un token antes de validar la sesión.
  const teniaToken = !!localStorage.getItem('token');

  // Permitir acceso únicamente con una sesión vigente.
  if (authService.isLoggedIn()) {
    return true;
  }

  // Si existía un token, pero ya venció, informar al usuario.
  const queryParams = teniaToken
    ? { sessionExpired: 'true' }
    : {};

  // Redirigir al inicio de sesión.
  return router.createUrlTree(
    ['/login'],
    { queryParams }
  );
};


