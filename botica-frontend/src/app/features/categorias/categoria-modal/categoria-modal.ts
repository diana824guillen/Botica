import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { finalize } from 'rxjs';

import {
  Categoria,
  CategoriaRequest
} from '../../../core/models/categoria.model';

import { CategoriaService } from '../services/categoria';

@Component({
  selector: 'app-categoria-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './categoria-modal.html',
  styleUrls: ['./categoria-modal.scss']
})
export class CategoriaModalComponent {

  @Input() categoriaEditar: Categoria | null = null;

  @Output() cerrar = new EventEmitter<void>();
  @Output() guardado = new EventEmitter<void>();

  categoriaForm!: FormGroup;

  isLoading = false;
  errorMessage = '';

  private fb = inject(FormBuilder);
  private categoriaService = inject(CategoriaService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {

    this.categoriaForm = this.fb.group({
      nombre: [
        '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(/\S/)
        ]
      ],

      descripcion: [
        '',
        [
          Validators.maxLength(255)
        ]
      ]
    });

    if (this.categoriaEditar) {
      this.categoriaForm.patchValue({
        nombre: this.categoriaEditar.nombre,
        descripcion: this.categoriaEditar.descripcion || ''
      });
    }
  }

  guardar(): void {

    if (this.isLoading) {
      return;
    }

    if (this.categoriaForm.invalid) {
      this.categoriaForm.markAllAsTouched();
      return;
    }

    const valores = this.categoriaForm.getRawValue();

    const nombre = String(valores.nombre ?? '')
      .trim()
      .replace(/\s+/g, ' ');

    const descripcion = String(valores.descripcion ?? '')
      .trim();

    if (!nombre) {
      this.categoriaForm.get('nombre')?.setErrors({
        required: true
      });

      this.categoriaForm.markAllAsTouched();
      return;
    }

    const datos: CategoriaRequest = {
      nombre,
      descripcion: descripcion || null
    };

    this.errorMessage = '';
    this.isLoading = true;

    const peticion = this.categoriaEditar
      ? this.categoriaService.actualizarCategoria(
          this.categoriaEditar.id,
          datos
        )
      : this.categoriaService.crearCategoria(datos);

    peticion
      .pipe(
        finalize(() => {
          this.isLoading = false;
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

    this.isLoading = false;

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje ??
      cuerpo?.message ??
      (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    const mensajeNormalizado = mensaje.toLowerCase();

    if (
      err?.status === 409 ||
      mensajeNormalizado.includes('ya existe una categoría') ||
      mensajeNormalizado.includes('ya existe otra categoría')
    ) {

      this.errorMessage =
        'Ya existe una categoría con ese nombre. Ingrese uno diferente.';

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
        'Ocurrió un error al guardar la categoría. Inténtelo nuevamente.';
    }

    this.cdr.detectChanges();
  }
}


