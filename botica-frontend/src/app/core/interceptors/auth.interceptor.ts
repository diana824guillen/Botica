import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import {
  catchError,
  finalize,
  throwError
} from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);

  // El login no necesita token.
  const esLogin = req.url.includes('/auth/login');

  if (esLogin) {
    return next(req);
  }

  const token = localStorage.getItem('token');

  const solicitudProtegida = !!token;

  // Agregar el token a la solicitud.
  const request = token
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      })
    : req;

  // Registrar solicitudes que ya comenzaron.
  if (solicitudProtegida) {
    authService.iniciarSolicitudProtegida();
  }

  return next(request).pipe(

    catchError((error: unknown) => {

      if (
        solicitudProtegida &&
        error instanceof HttpErrorResponse &&
        error.status === 401
      ) {

        // El backend rechazó la autenticación. Se solicita el cierre de sesión, pero el servicio
        // espera las demás solicitudes que ya están en curso.
        authService.solicitarCierrePorAutenticacion();
      }

      // Cada componente puede continuar gestionando su error.
      return throwError(() => error);
    }),

    finalize(() => {

      if (solicitudProtegida) {
        authService.finalizarSolicitudProtegida();
      }

    })

  );
};


