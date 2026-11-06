import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const authGuard = () => {

  const authService = inject(AuthService);
  const router = inject(Router);

  // Validar primero la sesión.
  const sesionValida = authService.isLoggedIn();

  // Si hay operaciones en curso durante el vencimiento, cancelar la navegación y mantener la pantalla actual.
  if (authService.hayOperacionesPendientesDeCierre()) {
    return false;
  }

  if (sesionValida) {
    return true;
  }

  const teniaToken = !!localStorage.getItem('token');

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


