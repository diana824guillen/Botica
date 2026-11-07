import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  VentaPrevisualizacionRequest,
  VentaPrevisualizacionResponse,
  VentaRequest,
  VentaResponse,
  VentaListado,
  PaginaResponse
} from '../../../core/models/venta.model';

@Injectable({
  providedIn: 'root'
})
export class VentaService {

  private apiUrl = `${environment.apiUrl}/ventas`;

  constructor(private http: HttpClient) {}

  // ============================================================
  // PREVISUALIZAR VENTA
  // ============================================================

  previsualizarVenta(
    request: VentaPrevisualizacionRequest
  ): Observable<VentaPrevisualizacionResponse> {

    return this.http.post<VentaPrevisualizacionResponse>(
      `${this.apiUrl}/previsualizacion`,
      request
    );
  }

  // ============================================================
  // REGISTRAR VENTA
  // ============================================================

  registrarVenta(
    request: VentaRequest
  ): Observable<VentaResponse> {

    return this.http.post<VentaResponse>(
      this.apiUrl,
      request
    );
  }

  // ============================================================
  // HISTORIAL GENERAL DE VENTAS
  // ============================================================

  listarVentas(
    pagina: number = 0,
    tamano: number = 10,
    buscar: string = '',
    desde: string = '',
    hasta: string = ''
  ): Observable<PaginaResponse<VentaListado>> {

    let params = new HttpParams()
      .set('pagina', pagina)
      .set('tamano', tamano);

    const criterio = buscar.trim();

    if (criterio) {
      params = params.set('buscar', criterio);
    }

    if (desde) {
      params = params.set('desde', desde);
    }

    if (hasta) {
      params = params.set('hasta', hasta);
    }

    return this.http.get<PaginaResponse<VentaListado>>(
      this.apiUrl,
      { params }
    );
  }

  // ============================================================
  // CONSULTAR DETALLE COMPLETO DE UNA VENTA
  // ============================================================

  obtenerVentaPorId(
    idVenta: number
  ): Observable<VentaResponse> {

    return this.http.get<VentaResponse>(
      `${this.apiUrl}/${idVenta}`
    );
  }
}


