import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  Lote,
  LoteAjusteRequest,
  LoteRequest
} from '../../../core/models/lote.model';

@Injectable({
  providedIn: 'root'
})
export class LoteService {

  private apiUrl = `${environment.apiUrl}/lotes`;
  private productosUrl = `${environment.apiUrl}/productos`;

  constructor(private http: HttpClient) {}

  // Listado general de lotes.
  listarLotes(): Observable<Lote[]> {
    return this.http.get<Lote[]>(this.apiUrl);
  }

  // Listado de lotes correspondientes a un producto.
  listarLotesPorProducto(idProducto: number): Observable<Lote[]> {
    return this.http.get<Lote[]>(
      `${this.productosUrl}/${idProducto}/lotes`
    );
  }

  // Registrar un ingreso de lote.
  crearLote(
    idProducto: number,
    request: LoteRequest
  ): Observable<Lote> {
    return this.http.post<Lote>(
      `${this.productosUrl}/${idProducto}/lotes`,
      request
    );
  }

  // Ajustar o reducir las existencias de un lote.
  ajustarLote(
    idLote: number,
    request: LoteAjusteRequest
  ): Observable<Lote> {
    return this.http.patch<Lote>(
      `${this.apiUrl}/${idLote}/ajustar`,
      request
    );
  }

  // Activar o desactivar un lote.
  cambiarEstado(
    idLote: number,
    estado: number
  ): Observable<Lote> {
    return this.http.patch<Lote>(
      `${this.apiUrl}/${idLote}/estado?estado=${estado}`,
      {}
    );
  }

  // Eliminación lógica del lote.
  eliminarLote(idLote: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${idLote}`
    );
  }
}


