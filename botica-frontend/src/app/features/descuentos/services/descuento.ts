import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Descuento,
  DescuentoRequest,
  DescuentoUpdateRequest
} from '../../../core/models/descuento.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DescuentoService {

  private apiUrl = `${environment.apiUrl}/descuentos`;

  constructor(private http: HttpClient) {}

  listarDescuentos(): Observable<Descuento[]> {
    return this.http.get<Descuento[]>(this.apiUrl);
  }

  obtenerDescuento(id: number): Observable<Descuento> {
    return this.http.get<Descuento>(`${this.apiUrl}/${id}`);
  }

  crearDescuento(
    descuento: DescuentoRequest
  ): Observable<Descuento> {
    return this.http.post<Descuento>(
      this.apiUrl,
      descuento
    );
  }

  actualizarDescuento(
    id: number,
    descuento: DescuentoUpdateRequest
  ): Observable<Descuento> {
    return this.http.put<Descuento>(
      `${this.apiUrl}/${id}`,
      descuento
    );
  }

  cambiarEstado(
    id: number,
    estado: number
  ): Observable<Descuento> {
    return this.http.patch<Descuento>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarDescuento(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}


