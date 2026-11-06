import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);
  const router = inject(Router);

  // Identificar las solicitudes de inicio de sesión.
  const esLogin = req.url.includes('/auth/login');

  // El login no necesita token ni debe activar el cierre de sesión por credenciales incorrectas.
  if (esLogin) {
    return next(req);
  }

  // Obtener el token almacenado.
  const token = localStorage.getItem('token');

  // Agregar el token a las solicitudes protegidas.
  const request = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  return next(request).pipe(

    catchError((error: unknown) => {

      // Gestionar únicamente los errores de autenticación.
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401
      ) {

        // Comprobar si existía una sesión almacenada.
        const teniaToken = !!localStorage.getItem('token');

        // Eliminar los datos de la sesión.
        authService.logout();

        // Redirigir al login si todavía no estamos allí.
        if (!router.url.startsWith('/login')) {

          const queryParams = teniaToken
            ? { sessionExpired: 'true' }
            : {};

          void router.navigate(
            ['/login'],
            { queryParams }
          );
        }
      }

      // Permitir que cada componente gestione su propio error.
      return throwError(() => error);
    })

  );
};


