import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Lote,
  LoteRequest,
  LoteAjusteRequest
} from '../../../core/models/lote.model';

import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoteService {

  private apiUrl = `${environment.apiUrl}/lotes`;
  private productosUrl = `${environment.apiUrl}/productos`;

  constructor(private http: HttpClient) {}

  listarLotes(): Observable<Lote[]> {
    return this.http.get<Lote[]>(this.apiUrl);
  }

  listarLotesPorProducto(idProducto: number): Observable<Lote[]> {
    return this.http.get<Lote[]>(
      `${this.productosUrl}/${idProducto}/lotes`
    );
  }

  obtenerLote(id: number): Observable<Lote> {
    return this.http.get<Lote>(`${this.apiUrl}/${id}`);
  }

  crearLote(
    idProducto: number,
    request: LoteRequest
  ): Observable<Lote> {
    return this.http.post<Lote>(
      `${this.productosUrl}/${idProducto}/lotes`,
      request
    );
  }

  listarLotesPorVencer(dias = 30): Observable<Lote[]> {
    return this.http.get<Lote[]>(
      `${this.apiUrl}/por-vencer?dias=${dias}`
    );
  }

  listarLotesVencidos(): Observable<Lote[]> {
    return this.http.get<Lote[]>(
      `${this.apiUrl}/vencidos`
    );
  }

  ajustarLote(
    id: number,
    request: LoteAjusteRequest
  ): Observable<Lote> {
    return this.http.patch<Lote>(
      `${this.apiUrl}/${id}/ajustar`,
      request
    );
  }

  cambiarEstado(id: number, estado: number): Observable<Lote> {
    return this.http.patch<Lote>(
      `${this.apiUrl}/${id}/estado?estado=${estado}`,
      {}
    );
  }

  eliminarLote(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}


