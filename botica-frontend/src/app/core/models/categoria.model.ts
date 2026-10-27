export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
  estado: number;
}

export interface CategoriaRequest {
  nombre: string;
  descripcion: string | null;
}


