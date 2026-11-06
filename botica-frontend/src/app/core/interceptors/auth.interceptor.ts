import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';

import { inject } from '@angular/core';

import {
  catchError,
  finalize,
  throwError
} from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  // El inicio de sesión no necesita un token anterior.
  const esLogin = req.url.includes('/auth/login');

  if (esLogin) {
    return next(req);
  }

  const token = localStorage.getItem('token');

  // Si no existe token, dejar que Spring Security responda según las reglas de autenticación.
  if (!token) {
    return next(req);
  }

  // Evitar solicitudes nuevas si el token ya venció o existe un cierre de sesión pendiente.
  const solicitudIniciada =
    authService.iniciarSolicitudProtegida(token);

  if (!solicitudIniciada) {

    return throwError(() =>
      new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
        url: req.url,
        error: {
          status: 401,
          mensaje: 'La sesión ha vencido. Inicia sesión nuevamente.'
        }
      })
    );
  }

  // Adjuntar el JWT a la petición.
  const requestAutenticada = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(requestAutenticada).pipe(

    catchError((error: unknown) => {

      if (
        error instanceof HttpErrorResponse &&
        error.status === 401
      ) {

        authService.solicitarCierrePorAutenticacion(token);
      }

      // Permitir que cada módulo gestione su error.
      return throwError(() => error);
    }),

    finalize(() => {

      // Liberar la solicitud cuando termina.
      authService.finalizarSolicitudProtegida(token);

    })

  );
};


