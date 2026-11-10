export interface Perfil {
  id: number;
  nombre: string;
  descripcion: string | null;
  estado: number;
  totalUsuarios: number;
  totalPermisos: number;
  opcionIds: number[];
  protegido: boolean;
}

export interface OpcionPermiso {
  id: number;
  nombre: string;
  ruta: string | null;
  icono: string | null;
  modulo: string;
  accion: string;
  estado: number;
}

export interface PerfilRequest {
  nombre: string;
  descripcion: string | null;
}

export interface PerfilPermisosRequest {
  opcionIds: number[];
}


