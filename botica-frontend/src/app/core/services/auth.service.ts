import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';

// Interfaz basada en el LoginResponse del Backend.
export interface LoginResponse {
  token: string;
  username: string;
  perfil: string;
  tipoDocumento: string;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  permisos: string[];
}

interface JwtPayload {
  exp?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  // Iniciar sesión.
  login(credentials: any): Observable<LoginResponse> {

    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {

          // Guardar el token y los datos del usuario.
          localStorage.setItem('token', response.token);

          localStorage.setItem(
            'user',
            JSON.stringify(response)
          );

        })
      );
  }

  // Cerrar sesión y eliminar los datos almacenados.
  logout(): void {

    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  // Comprobar si existe una sesión válida.
  isLoggedIn(): boolean {

    const token = localStorage.getItem('token');

    if (!token) {
      return false;
    }

    // Comprobar la fecha de vencimiento del token.
    if (this.isTokenExpired(token)) {

      this.logout();

      return false;
    }

    return true;
  }

  // Comprobar si el JWT ha vencido.
  private isTokenExpired(token: string): boolean {

    try {

      const parts = token.split('.');

      if (parts.length !== 3) {
        return true;
      }

      // Decodificar el contenido del JWT.
      const payloadBase64 = parts[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const payloadPadded = payloadBase64.padEnd(
        Math.ceil(payloadBase64.length / 4) * 4,
        '='
      );

      const payload: JwtPayload = JSON.parse(
        atob(payloadPadded)
      );

      // Un token sin fecha de expiración válida se considera inválido.
      if (
        typeof payload.exp !== 'number' ||
        !Number.isFinite(payload.exp)
      ) {
        return true;
      }

      // El JWT expresa su expiración en segundos.
      const fechaExpiracion = payload.exp * 1000;

      return Date.now() >= fechaExpiracion;

    } catch {

      // Si no se puede interpretar el token, se considera inválido.
      return true;
    }
  }

  // Obtener el perfil del usuario autenticado.
  getUserRole(): string {

    const user = localStorage.getItem('user');

    if (user) {
      return JSON.parse(user).perfil;
    }

    return '';
  }
}


