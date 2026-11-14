import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  MovimientoInventario,
  PaginaMovimientoResponse,
  TipoMovimientoInventario
} from '../../../core/models/movimiento-inventario.model';

@Injectable({
  providedIn: 'root'
})
export class MovimientoInventarioService {

  private apiUrl = `${environment.apiUrl}/movimientos`;

  constructor(private http: HttpClient) {}

  // ============================================================
  // LISTAR MOVIMIENTOS DE INVENTARIO
  // ============================================================

  listarMovimientos(
    pagina: number = 0,
    tamano: number = 10,
    buscar: string = '',
    tipo: TipoMovimientoInventario | '' = '',
    desde: string = '',
    hasta: string = ''
  ): Observable<PaginaMovimientoResponse<MovimientoInventario>> {

    let params = new HttpParams()
      .set('pagina', pagina)
      .set('tamano', tamano);

    const criterio = buscar.trim();

    if (criterio) {
      params = params.set('buscar', criterio);
    }

    if (tipo) {
      params = params.set('tipo', tipo);
    }

    if (desde) {
      params = params.set('desde', desde);
    }

    if (hasta) {
      params = params.set('hasta', hasta);
    }

    return this.http.get<PaginaMovimientoResponse<MovimientoInventario>>(
      this.apiUrl,
      { params }
    );
  }
}


