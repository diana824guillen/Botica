import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Usuario,
  UsuarioRequest,
  UsuarioUpdate,
  UsuarioPageResponse
} from '../../../core/models/usuario.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {

  private readonly apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(
    private readonly http: HttpClient
  ) {}

  // ============================================================
  // LISTAR USUARIOS CON PAGINACIÓN Y BÚSQUEDA
  // ============================================================

  listarUsuarios(
    page: number,
    size: number,
    search: string
  ): Observable<UsuarioPageResponse> {

    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('search', search.trim());

    return this.http.get<UsuarioPageResponse>(
      this.apiUrl,
      { params }
    );
  }

  // ============================================================
  // OBTENER USUARIO POR ID
  // ============================================================

  obtenerUsuario(id: number): Observable<Usuario> {

    return this.http.get<Usuario>(
      `${this.apiUrl}/${id}`
    );
  }

  // ============================================================
  // REGISTRAR USUARIO
  // ============================================================

  registrarUsuario(
    datos: UsuarioRequest
  ): Observable<Usuario> {

    return this.http.post<Usuario>(
      this.apiUrl,
      datos
    );
  }

  // ============================================================
  // ACTUALIZAR USUARIO
  // ============================================================

  actualizarUsuario(
    id: number,
    datos: UsuarioUpdate
  ): Observable<Usuario> {

    return this.http.put<Usuario>(
      `${this.apiUrl}/${id}`,
      datos
    );
  }

  // ============================================================
  // ACTIVAR / DESACTIVAR USUARIO
  // ============================================================

  cambiarEstado(
    id: number,
    estado: number
  ): Observable<Usuario> {

    return this.http.patch<Usuario>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  // ============================================================
  // RESTABLECER CONTRASEÑA
  // ============================================================

  restablecerClave(
    id: number,
    nuevaClave: string
  ): Observable<Usuario> {

    return this.http.patch<Usuario>(
      `${this.apiUrl}/${id}/clave`,
      { nuevaClave }
    );
  }

  // ============================================================
  // ELIMINACIÓN LÓGICA
  // ============================================================

  eliminarUsuario(id: number): Observable<void> {

    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }

}


