import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Si la petición va al login, la dejamos pasar SIN el token
  if (req.url.includes('/auth/login')) {
    return next(req);
  }

  // 2. Para las demás peticiones, inyectamos el token
  const token = localStorage.getItem('token');
  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }
  
  return next(req);
};


