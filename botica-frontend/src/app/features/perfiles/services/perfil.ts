import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Perfil,
  OpcionPermiso,
  PerfilRequest,
  PerfilPermisosRequest
} from '../../../core/models/perfil.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PerfilService {

  private apiUrl = `${environment.apiUrl}/perfiles`;

  constructor(private http: HttpClient) {}

  listarPerfiles(): Observable<Perfil[]> {
    return this.http.get<Perfil[]>(this.apiUrl);
  }

  obtenerPerfil(id: number): Observable<Perfil> {
    return this.http.get<Perfil>(`${this.apiUrl}/${id}`);
  }

  listarOpciones(): Observable<OpcionPermiso[]> {
    return this.http.get<OpcionPermiso[]>(
      `${this.apiUrl}/opciones`
    );
  }

  crearPerfil(datos: PerfilRequest): Observable<Perfil> {
    return this.http.post<Perfil>(this.apiUrl, datos);
  }

  actualizarPerfil(
    id: number,
    datos: PerfilRequest
  ): Observable<Perfil> {
    return this.http.put<Perfil>(
      `${this.apiUrl}/${id}`,
      datos
    );
  }

  actualizarPermisos(
    id: number,
    datos: PerfilPermisosRequest
  ): Observable<Perfil> {
    return this.http.put<Perfil>(
      `${this.apiUrl}/${id}/permisos`,
      datos
    );
  }

  cambiarEstado(
    id: number,
    estado: number
  ): Observable<Perfil> {
    return this.http.patch<Perfil>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarPerfil(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}


