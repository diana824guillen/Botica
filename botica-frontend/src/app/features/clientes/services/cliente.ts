import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente, ClienteRequest } from '../../../core/models/cliente.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private apiUrl = `${environment.apiUrl}/clientes`;

  constructor(private http: HttpClient) {}

  listarClientes(page: number, size: number, search: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}?page=${page}&size=${size}&search=${search}`);
  }

  buscarPorDocumento(documento: string): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.apiUrl}/${documento}`);
  }

  registrarCliente(cliente: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.apiUrl, cliente);
  }

  actualizarCliente(id: number, cliente: any): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/${id}`, cliente);
  }

  eliminarCliente(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  cambiarEstado(id: number, estado: number): Observable<Cliente> {
    return this.http.patch<Cliente>(`${this.apiUrl}/${id}/estado?estado=${estado}`, {});
  }
}


