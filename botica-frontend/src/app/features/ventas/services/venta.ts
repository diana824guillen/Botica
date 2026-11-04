import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  VentaPrevisualizacionRequest,
  VentaPrevisualizacionResponse,
  VentaRequest,
  VentaResponse
} from '../../../core/models/venta.model';

@Injectable({
  providedIn: 'root'
})
export class VentaService {

  private apiUrl = `${environment.apiUrl}/ventas`;

  constructor(private http: HttpClient) {}

  // Previsualizar una venta sin registrar ni modificar el inventario.
  previsualizarVenta(
    request: VentaPrevisualizacionRequest
  ): Observable<VentaPrevisualizacionResponse> {

    return this.http.post<VentaPrevisualizacionResponse>(
      `${this.apiUrl}/previsualizacion`,
      request
    );
  }

  // Registrar definitivamente una venta.
  registrarVenta(
    request: VentaRequest
  ): Observable<VentaResponse> {

    return this.http.post<VentaResponse>(
      this.apiUrl,
      request
    );
  }
}


