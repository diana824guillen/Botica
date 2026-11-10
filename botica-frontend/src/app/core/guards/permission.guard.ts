import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const permissionGuard = (permission: string) => {

  return () => {

    const authService = inject(AuthService);

    const router = inject(Router);

    return authService.hasPermission(permission)
      ? true
      : router.createUrlTree(['/dashboard']);
  };

};


