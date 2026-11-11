import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const permissionGuard = (permission: string) => {

  return () => {

    const authService = inject(AuthService);

    const router = inject(Router);

    // Consultar los permisos directamente al backend antes de permitir el ingreso a la ruta.
    return authService.sincronizarPermisos().pipe(

      map(permisos => {

        const esAdministrador =
          authService.getUserRole() === 'ADMIN';

        const tieneAcceso =
          esAdministrador || permisos.includes(permission);

        return tieneAcceso
          ? true
          : router.createUrlTree(['/dashboard']);
      }),

      catchError(() =>
        of(router.createUrlTree(['/dashboard']))
      )
    );
  };

};


