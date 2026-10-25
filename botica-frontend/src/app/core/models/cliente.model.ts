export interface Cliente {
  id: number;
  tipoDocumento: 'DNI' | 'CE' | 'PASAPORTE';
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  correo: string | null;
  estado: number;
}

export interface ClienteRequest {
  numeroDocumento: string;
  nombres?: string;
  apellidos?: string;
  correo?: string;
}


