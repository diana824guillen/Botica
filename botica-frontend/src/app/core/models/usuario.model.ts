export type TipoDocumentoUsuario = 'DNI' | 'CE' | 'PASAPORTE';

export interface Usuario {
  id: number;

  perfilId: number;
  perfilNombre: string;

  tipoDocumento: TipoDocumentoUsuario;
  numeroDocumento: string;

  nombres: string;
  apellidos: string;

  username: string;
  correo: string | null;

  estado: number;
  fechaCreacion: string | null;

  bloqueado: boolean;
  bloqueoHasta: string | null;

  protegido: boolean;
}

export interface UsuarioRequest {
  perfilId: number;
  tipoDocumento: TipoDocumentoUsuario;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  username: string;
  clave: string;
  correo: string | null;
}

export interface UsuarioUpdate {
  perfilId: number;
  tipoDocumento: TipoDocumentoUsuario;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  username: string;
  correo: string | null;
}

export interface UsuarioClaveRequest {
  nuevaClave: string;
}

export interface UsuarioPageResponse {
  content: Usuario[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}


