import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Categoria,
  CategoriaRequest
} from '../../../core/models/categoria.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {

  private apiUrl = `${environment.apiUrl}/categorias`;

  constructor(private http: HttpClient) {}

  listarCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.apiUrl);
  }

  obtenerCategoria(id: number): Observable<Categoria> {
    return this.http.get<Categoria>(`${this.apiUrl}/${id}`);
  }

  crearCategoria(
    categoria: CategoriaRequest
  ): Observable<Categoria> {
    return this.http.post<Categoria>(this.apiUrl, categoria);
  }

  actualizarCategoria(
    id: number,
    categoria: CategoriaRequest
  ): Observable<Categoria> {
    return this.http.put<Categoria>(
      `${this.apiUrl}/${id}`,
      categoria
    );
  }

  cambiarEstado(
    id: number,
    estado: number
  ): Observable<Categoria> {
    return this.http.patch<Categoria>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}


