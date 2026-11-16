import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  DashboardVentasResponse,
  DashboardInventarioResponse
} from '../../../core/models/dashboard.model';

import {
  MovimientoInventario
} from '../../../core/models/movimiento-inventario.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(
    private readonly http: HttpClient
  ) {}

  // ============================================================
  // RESUMEN DE VENTAS
  // ============================================================

  obtenerResumenVentas(
    desde: string,
    hasta: string,
    desdeComparacion: string,
    hastaComparacion: string
  ): Observable<DashboardVentasResponse> {

    const params = new HttpParams()
      .set('desde', desde)
      .set('hasta', hasta)
      .set('desdeComparacion', desdeComparacion)
      .set('hastaComparacion', hastaComparacion);

    return this.http.get<DashboardVentasResponse>(
      `${this.apiUrl}/ventas`,
      { params }
    );
  }

  // ============================================================
  // RESUMEN DE INVENTARIO
  // ============================================================

  obtenerResumenInventario(
    dias: 7 | 15 | 30 = 30
  ): Observable<DashboardInventarioResponse> {

    const params = new HttpParams()
      .set('dias', dias);

    return this.http.get<DashboardInventarioResponse>(
      `${this.apiUrl}/inventario`,
      { params }
    );
  }

  // ============================================================
  // ÚLTIMOS MOVIMIENTOS DE INVENTARIO
  // ============================================================

  listarMovimientosRecientes(): Observable<MovimientoInventario[]> {

    return this.http.get<MovimientoInventario[]>(
      `${this.apiUrl}/movimientos-recientes`
    );
  }

}


