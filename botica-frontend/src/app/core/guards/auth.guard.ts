import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const authGuard = () => {

  const authService = inject(AuthService);
  const router = inject(Router);

  // Comprobar si existía un token.
  const teniaToken = !!localStorage.getItem('token');

  // Permitir acceso solamente con una sesión vigente.
  if (authService.isLoggedIn()) {
    return true;
  }

  // Diferenciar el acceso sin sesión del vencimiento del JWT.
  const queryParams = (
    teniaToken || authService.sesionExpirada()
  )
    ? { sessionExpired: 'true' }
    : {};

  return router.createUrlTree(
    ['/login'],
    { queryParams }
  );
};


