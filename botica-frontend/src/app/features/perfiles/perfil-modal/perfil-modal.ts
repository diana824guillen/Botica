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
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { finalize } from 'rxjs';

import {
  Perfil,
  PerfilRequest
} from '../../../core/models/perfil.model';

import { PerfilService } from '../services/perfil';

@Component({
  selector: 'app-perfil-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './perfil-modal.html',
  styleUrls: ['./perfil-modal.scss']
})
export class PerfilModalComponent implements OnInit {

  @Input() perfilEditar: Perfil | null = null;

  @Output() cerrar = new EventEmitter<void>();

  @Output() guardado = new EventEmitter<void>();

  perfilForm!: FormGroup;

  isSaving = false;

  errorMessage = '';

  private readonly fb = inject(FormBuilder);

  private readonly perfilService = inject(PerfilService);

  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {

    this.perfilForm = this.fb.group({

      nombre: [
        this.perfilEditar?.nombre ?? '',
        [
          Validators.required,
          Validators.maxLength(50),
          Validators.pattern(/\S/)
        ]
      ],

      descripcion: [
        this.perfilEditar?.descripcion ?? '',
        [
          Validators.maxLength(255)
        ]
      ]

    });
  }

  // ============================================================
  // CERRAR MODAL
  // ============================================================

  cerrarModal(): void {

    if (this.isSaving) {
      return;
    }

    this.cerrar.emit();
  }

  // ============================================================
  // GUARDAR PERFIL
  // ============================================================

  guardar(): void {

    if (this.isSaving) {
      return;
    }

    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      return;
    }

    const valores = this.perfilForm.getRawValue();

    const nombre = String(valores.nombre ?? '')
      .trim()
      .replace(/\s+/g, ' ');

    const descripcion = String(
      valores.descripcion ?? ''
    ).trim();

    if (!nombre) {

      this.perfilForm.get('nombre')?.setErrors({
        pattern: true
      });

      this.perfilForm.markAllAsTouched();

      return;
    }

    const datos: PerfilRequest = {
      nombre,
      descripcion: descripcion || null
    };

    this.errorMessage = '';
    this.isSaving = true;

    const peticion = this.perfilEditar
      ? this.perfilService.actualizarPerfil(
          this.perfilEditar.id,
          datos
        )
      : this.perfilService.crearPerfil(datos);

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

        error: err => {

          console.error(
            'Error al guardar el perfil:',
            err
          );

          this.manejarError(err);
        }

      });
  }

  // ============================================================
  // MANEJAR ERRORES
  // ============================================================

  private manejarError(err: any): void {

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje
      ?? cuerpo?.message
      ?? (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    if (err?.status === 409) {

      this.errorMessage = mensaje
        || 'Ya existe un perfil con ese nombre. Ingrese uno diferente.';

    } else if (err?.status === 403) {

      this.errorMessage = mensaje
        || 'No tiene permisos para realizar esta operación.';

    } else if (err?.status === 0) {

      this.errorMessage =
        'No se pudo conectar con el servidor. Inténtelo nuevamente.';

    } else {

      this.errorMessage = mensaje
        || 'Ocurrió un error al guardar el perfil. Inténtelo nuevamente.';
    }

    this.cdr.detectChanges();
  }

}


