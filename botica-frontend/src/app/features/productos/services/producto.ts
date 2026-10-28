import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Producto,
  ProductoRequest,
  ProductoUpdateRequest,
  Presentacion,
  PresentacionRequest
} from '../../../core/models/producto.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private apiUrl = `${environment.apiUrl}/productos`;

  constructor(private http: HttpClient) {}

  // Productos
  listarProductos(categoriaId?: number): Observable<Producto[]> {

    const url = categoriaId != null
      ? `${this.apiUrl}?categoriaId=${categoriaId}`
      : this.apiUrl;

    return this.http.get<Producto[]>(url);
  }

  obtenerProducto(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.apiUrl}/${id}`);
  }

  crearProducto(producto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.apiUrl, producto);
  }

  actualizarProducto(
    id: number,
    producto: ProductoUpdateRequest
  ): Observable<Producto> {
    return this.http.put<Producto>(
      `${this.apiUrl}/${id}`,
      producto
    );
  }

  cambiarEstado(
    id: number,
    estado: number
  ): Observable<Producto> {
    return this.http.patch<Producto>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Presentaciones
  listarPresentaciones(idProducto: number): Observable<Presentacion[]> {
    return this.http.get<Presentacion[]>(
      `${this.apiUrl}/${idProducto}/presentaciones`
    );
  }

  agregarPresentaciones(
    idProducto: number,
    presentaciones: PresentacionRequest[]
  ): Observable<Producto> {
    return this.http.post<Producto>(
      `${this.apiUrl}/${idProducto}/presentaciones`,
      presentaciones
    );
  }

  actualizarPresentacion(
    idProducto: number,
    idPresentacion: number,
    presentacion: PresentacionRequest
  ): Observable<Presentacion> {
    return this.http.put<Presentacion>(
      `${this.apiUrl}/${idProducto}/presentaciones/${idPresentacion}`,
      presentacion
    );
  }

  cambiarEstadoPresentacion(
    idProducto: number,
    idPresentacion: number,
    estado: number
  ): Observable<Producto> {
    return this.http.patch<Producto>(
      `${this.apiUrl}/${idProducto}/presentaciones/${idPresentacion}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarPresentacion(
    idProducto: number,
    idPresentacion: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${idProducto}/presentaciones/${idPresentacion}`
    );
  }
}


