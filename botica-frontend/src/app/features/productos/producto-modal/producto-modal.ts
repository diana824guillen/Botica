import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { finalize, Observable } from 'rxjs';

import {
  Producto,
  ProductoRequest,
  ProductoUpdateRequest,
  TipoPresentacion
} from '../../../core/models/producto.model';

import { Categoria } from '../../../core/models/categoria.model';

import { CategoriaService } from '../../categorias/services/categoria';
import { ProductoService } from '../services/producto';

@Component({
  selector: 'app-producto-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './producto-modal.html',
  styleUrls: ['./producto-modal.scss']
})
export class ProductoModalComponent implements OnInit {

  @Input() productoEditar: Producto | null = null;

  @Output() cerrar = new EventEmitter<void>();
  @Output() guardado = new EventEmitter<void>();

  productoForm!: FormGroup;

  categorias: Categoria[] = [];

  isSaving = false;
  cargandoCategorias = false;
  errorMessage = '';

  tiposPresentacion: { value: TipoPresentacion; label: string }[] = [
    { value: 'UNIDAD', label: 'Unidad' },
    { value: 'BLISTER', label: 'Blíster' },
    { value: 'CAJA', label: 'Caja' },
    { value: 'FRASCO', label: 'Frasco' },
    { value: 'TIRA', label: 'Tira' },
    { value: 'OTRO', label: 'Otro' }
  ];

  private fb = inject(FormBuilder);
  private categoriaService = inject(CategoriaService);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarCategorias();
  }

  get presentacionesForm(): FormArray {
    return this.productoForm.get('presentaciones') as FormArray;
  }

  inicializarFormulario(): void {

    this.productoForm = this.fb.group({

      categoriaId: [
        null,
        Validators.required
      ],

      nombre: [
        '',
        [
          Validators.required,
          Validators.maxLength(150),
          Validators.pattern(/\S/)
        ]
      ],

      descripcion: [''],

      requiereReceta: [false],

      stockMinimo: [
        0,
        [
          Validators.required,
          Validators.min(0)
        ]
      ],

      presentaciones: this.fb.array([])
    });

    if (this.productoEditar) {

      this.productoForm.patchValue({
        categoriaId: this.productoEditar.categoriaId,
        nombre: this.productoEditar.nombre,
        descripcion: this.productoEditar.descripcion || '',
        requiereReceta: this.productoEditar.requiereReceta,
        stockMinimo: this.productoEditar.stockMinimo
      });

    } else {

      // El producto nuevo debe tener al menos una presentación.
      this.agregarPresentacion();
    }
  }

  private crearGrupoPresentacion(): FormGroup {

    return this.fb.group({

      tipo: [
        'UNIDAD',
        Validators.required
      ],

      factorConversion: [
        1,
        [
          Validators.required,
          Validators.min(1),
          Validators.pattern(/^\d+$/)
        ]
      ],

      precioVenta: [
        null,
        [
          Validators.required,
          Validators.min(0.01)
        ]
      ]

    });
  }

  agregarPresentacion(): void {
    this.presentacionesForm.push(this.crearGrupoPresentacion());
  }

  quitarPresentacion(index: number): void {

    if (this.presentacionesForm.length > 1) {
      this.presentacionesForm.removeAt(index);
    }
  }

  cargarCategorias(): void {

    this.cargandoCategorias = true;

    this.categoriaService
      .listarCategorias()
      .pipe(
        finalize(() => {
          this.cargandoCategorias = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {

          const recibidas = respuesta || [];

          // Solo mostrar categorías activas para asignar nuevos productos.
          this.categorias = recibidas.filter(
            categoria => categoria.estado === 1
          );

          // Al editar, mantener visible la categoría ya asociada,
          // incluso si está inactiva.
          if (this.productoEditar) {

            const categoriaActual = recibidas.find(
              categoria =>
                categoria.id === this.productoEditar?.categoriaId
            );

            if (
              categoriaActual &&
              !this.categorias.some(
                categoria => categoria.id === categoriaActual.id
              )
            ) {
              this.categorias = [
                categoriaActual,
                ...this.categorias
              ];
            }
          }
        },

        error: (err) => {

          console.error('Error al cargar categorías:', err);

          this.errorMessage =
            err?.error?.mensaje ||
            'No se pudieron cargar las categorías. Inténtelo nuevamente.';
        }

      });
  }

  guardar(): void {

    if (this.isSaving || this.cargandoCategorias) {
      return;
    }

    if (this.productoForm.invalid) {
      this.productoForm.markAllAsTouched();
      return;
    }

    const valores = this.productoForm.getRawValue();

    const nombre = String(valores.nombre || '')
      .trim()
      .replace(/\s+/g, ' ');

    if (!nombre) {
      this.productoForm.get('nombre')?.setErrors({
        required: true
      });

      this.productoForm.markAllAsTouched();
      return;
    }

    const stockMinimo = Number(valores.stockMinimo);

    if (!Number.isFinite(stockMinimo) || stockMinimo < 0) {

      this.productoForm.get('stockMinimo')?.setErrors({
        min: true
      });

      return;
    }

    const categoriaId = Number(valores.categoriaId);

    let peticion: Observable<Producto>;

    if (this.productoEditar) {

      const datos: ProductoUpdateRequest = {
        categoriaId,
        nombre,
        descripcion: String(valores.descripcion || '').trim() || null,
        requiereReceta: Boolean(valores.requiereReceta),
        stockMinimo
      };

      peticion = this.productoService.actualizarProducto(
        this.productoEditar.id,
        datos
      );

    } else {

      const presentaciones = this.presentacionesForm.controls.map(
        control => ({
          tipo: control.get('tipo')?.value as TipoPresentacion,
          factorConversion: Number(
            control.get('factorConversion')?.value
          ),
          precioVenta: Number(control.get('precioVenta')?.value)
        })
      );

      // Evitar repetir el mismo tipo y factor dentro del formulario.
      const combinaciones = presentaciones.map(
        presentacion =>
          `${presentacion.tipo}|${presentacion.factorConversion}`
      );

      if (new Set(combinaciones).size !== combinaciones.length) {
        this.errorMessage =
          'No puede repetir el mismo tipo y factor de conversión. Revise las presentaciones.';
        return;
      }

      const datos: ProductoRequest = {
        categoriaId,
        nombre,
        descripcion: String(valores.descripcion || '').trim() || null,
        requiereReceta: Boolean(valores.requiereReceta),
        stockMinimo,
        presentaciones
      };

      peticion = this.productoService.crearProducto(datos);
    }

    this.errorMessage = '';
    this.isSaving = true;

    peticion
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {
          this.guardado.emit();
          this.cerrar.emit();
        },

        error: (err) => {
          this.manejarError(err);
        }

      });
  }

  manejarError(err: any): void {

    this.isSaving = false;

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje ??
      cuerpo?.message ??
      (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    const normalizado = mensaje.toLowerCase();

    if (
      err?.status === 409 ||
      normalizado.includes('ya existe un producto')
    ) {

      this.errorMessage =
        'Ya existe un producto con ese nombre. Ingrese otro nombre.';

    } else if (err?.status === 403) {

      this.errorMessage =
        mensaje || 'No tiene permisos para realizar esta operación.';

    } else if (err?.status === 0) {

      this.errorMessage =
        'No se pudo conectar con el servidor. Inténtelo nuevamente.';

    } else if (mensaje) {

      this.errorMessage = mensaje;

    } else {

      this.errorMessage =
        'Ocurrió un error al guardar el producto. Inténtelo nuevamente.';
    }

    this.cdr.detectChanges();
  }
}


