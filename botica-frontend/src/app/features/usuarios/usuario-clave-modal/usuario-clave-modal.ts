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

import type { Usuario } from '../../../core/models/usuario.model';

import { UsuarioService } from '../services/usuario';

@Component({
  selector: 'app-usuario-clave-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './usuario-clave-modal.html',
  styleUrls: ['./usuario-clave-modal.scss']
})
export class UsuarioClaveModalComponent {

  @Input() usuario: Usuario | null = null;

  @Output() cerrar = new EventEmitter<void>();

  @Output() guardado = new EventEmitter<void>();

  claveForm: FormGroup;

  isSaving = false;

  errorMessage = '';

  // Cada contraseña tiene su propio control de visibilidad.
  mostrarNuevaClave = false;

  mostrarConfirmarClave = false;

  private readonly fb = inject(FormBuilder);

  private readonly usuarioService = inject(UsuarioService);

  private readonly cdr = inject(ChangeDetectorRef);

  constructor() {

    this.claveForm = this.fb.group({

      nuevaClave: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72)
        ]
      ],

      confirmarClave: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72)
        ]
      ]

    });
  }

  // ============================================================
  // VISUALIZAR CONTRASEÑAS
  // ============================================================

  alternarVisibilidadNuevaClave(): void {
    this.mostrarNuevaClave = !this.mostrarNuevaClave;
  }

  alternarVisibilidadConfirmarClave(): void {
    this.mostrarConfirmarClave = !this.mostrarConfirmarClave;
  }

  // ============================================================
  // COMPROBAR SI LAS CONTRASEÑAS COINCIDEN
  // ============================================================

  get clavesCoinciden(): boolean {

    const nueva = this.claveForm.get('nuevaClave')?.value;

    const confirmar = this.claveForm.get('confirmarClave')?.value;

    return !!nueva && nueva === confirmar;
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
  // RESTABLECER CONTRASEÑA
  // ============================================================

  guardar(): void {

    if (this.isSaving) {
      return;
    }

    if (this.claveForm.invalid) {

      this.claveForm.markAllAsTouched();

      return;
    }

    if (!this.clavesCoinciden) {

      this.errorMessage =
        'Las contraseñas ingresadas no coinciden.';

      return;
    }

    if (!this.usuario) {

      this.errorMessage =
        'No se encontró el usuario que deseas modificar.';

      return;
    }

    const nuevaClave = String(
      this.claveForm.get('nuevaClave')?.value ?? ''
    );

    this.errorMessage = '';

    this.isSaving = true;

    this.usuarioService
      .restablecerClave(
        this.usuario.id,
        nuevaClave
      )
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
            'Error al restablecer contraseña:',
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

    if (err?.status === 403) {

      this.errorMessage = mensaje
        || 'No tiene permisos para realizar esta operación.';

    } else if (err?.status === 0) {

      this.errorMessage =
        'No se pudo conectar con el servidor. Inténtelo nuevamente.';

    } else {

      this.errorMessage = mensaje
        || 'No se pudo restablecer la contraseña. Inténtelo nuevamente.';
    }

    this.cdr.detectChanges();
  }

}


