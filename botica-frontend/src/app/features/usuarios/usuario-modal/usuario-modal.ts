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

import {
  Observable,
  finalize
} from 'rxjs';

import type {
  Usuario,
  UsuarioRequest,
  UsuarioUpdate
} from '../../../core/models/usuario.model';

import type { Perfil } from '../../../core/models/perfil.model';

import { UsuarioService } from '../services/usuario';

import { PerfilService } from '../../perfiles/services/perfil';

@Component({
  selector: 'app-usuario-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './usuario-modal.html',
  styleUrls: ['./usuario-modal.scss']
})
export class UsuarioModalComponent implements OnInit {

  @Input() usuarioEditar: Usuario | null = null;

  @Output() cerrar = new EventEmitter<void>();

  @Output() guardado = new EventEmitter<void>();

  usuarioForm!: FormGroup;

  perfiles: Perfil[] = [];

  isLoadingProfiles = false;

  perfilesError = '';

  isSaving = false;

  errorMessage = '';

  mostrarClave = false;

  private readonly fb = inject(FormBuilder);

  private readonly usuarioService = inject(UsuarioService);

  private readonly perfilService = inject(PerfilService);

  private readonly cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {

    this.inicializarFormulario();

    this.cargarPerfiles();
  }

  // ============================================================
  // FORMULARIO
  // ============================================================

  private inicializarFormulario(): void {

    this.usuarioForm = this.fb.group({

      perfilId: [
        {
          value: this.usuarioEditar?.perfilId ?? null,
          disabled: true
        },
        [
          Validators.required
        ]
      ],

      tipoDocumento: [
        this.usuarioEditar?.tipoDocumento ?? 'DNI',
        [
          Validators.required
        ]
      ],

      numeroDocumento: [
        this.usuarioEditar?.numeroDocumento ?? '',
        [
          Validators.required
        ]
      ],

      nombres: [
        this.usuarioEditar?.nombres ?? '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(
            /^(?=.*[a-zA-ZáéíóúÁÉÍÓÚñÑ])[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/
          )
        ]
      ],

      apellidos: [
        this.usuarioEditar?.apellidos ?? '',
        [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern(
            /^(?=.*[a-zA-ZáéíóúÁÉÍÓÚñÑ])[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/
          )
        ]
      ],

      username: [
        this.usuarioEditar?.username ?? '',
        [
          Validators.required,
          Validators.minLength(4),
          Validators.maxLength(50),
          Validators.pattern(/^[a-zA-Z0-9._-]+$/)
        ]
      ],

      clave: [
        '',
        this.usuarioEditar
          ? []
          : [
              Validators.required,
              Validators.minLength(8),
              Validators.maxLength(72)
            ]
      ],

      correo: [
        this.usuarioEditar?.correo ?? '',
        [
          Validators.maxLength(255),
          Validators.email
        ]
      ]

    });

    this.actualizarValidadoresDocumento();
  }

  // ============================================================
  // FILTRAR NÚMERO DE DOCUMENTO SEGÚN EL TIPO
  // ============================================================

  onInputDocumento(event: Event): void {

    const input = event.target as HTMLInputElement;

    const tipo = String(
      this.usuarioForm.get('tipoDocumento')?.value ?? 'DNI'
    );

    let valor = input.value;

    if (tipo === 'DNI') {

      valor = valor
        .replace(/[^0-9]/g, '')
        .substring(0, 8);

    } else if (tipo === 'CE') {

      valor = valor
        .replace(/[^a-zA-Z0-9]/g, '')
        .substring(0, 11)
        .toUpperCase();

    } else {

      valor = valor
        .replace(/[^a-zA-Z0-9]/g, '')
        .substring(0, 15)
        .toUpperCase();
    }

    if (input.value !== valor) {

      input.value = valor;

      this.usuarioForm
        .get('numeroDocumento')
        ?.setValue(valor);
    }
  }

  // ============================================================
  // CAMBIAR TIPO DE DOCUMENTO
  // ============================================================

  cambiarTipoDocumento(): void {

    // Al cambiar el tipo, se limpia el número anterior para
    // evitar conservar un documento con otro formato.
    this.usuarioForm
      .get('numeroDocumento')
      ?.reset('', { emitEvent: false });

    this.actualizarValidadoresDocumento();
  }

  // ============================================================
  // VALIDACIÓN SEGÚN TIPO DE DOCUMENTO
  // ============================================================

  actualizarValidadoresDocumento(): void {

    const tipo = String(
      this.usuarioForm?.get('tipoDocumento')?.value ?? 'DNI'
    );

    const control = this.usuarioForm?.get('numeroDocumento');

    if (!control) {
      return;
    }

    let formato: RegExp;

    switch (tipo) {

      case 'DNI':
        formato = /^\d{8}$/;
        break;

      case 'CE':
        formato = /^[A-Z0-9]{9,11}$/i;
        break;

      case 'PASAPORTE':
        formato = /^[A-Z0-9]{9,15}$/i;
        break;

      default:
        formato = /^$/;
        break;
    }

    control.setValidators([
      Validators.required,
      Validators.pattern(formato)
    ]);

    control.updateValueAndValidity();
  }

  // ============================================================
  // FILTRAR NOMBRES Y APELLIDOS
  // ============================================================

  onInputNombre(event: Event, controlName: string): void {

    const input = event.target as HTMLInputElement;

    const valor = input.value.replace(
      /[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g,
      ''
    );

    if (input.value !== valor) {

      input.value = valor;

      this.usuarioForm
        .get(controlName)
        ?.setValue(valor);
    }
  }

  // ============================================================
  // VISUALIZAR CONTRASEÑA
  // ============================================================

  alternarVisibilidadClave(): void {
    this.mostrarClave = !this.mostrarClave;
  }

  // ============================================================
  // CARGAR PERFILES DISPONIBLES
  // ============================================================

  private cargarPerfiles(): void {

    this.isLoadingProfiles = true;

    this.perfilesError = '';

    this.perfilService
      .listarPerfiles()
      .pipe(
        finalize(() => {

          this.isLoadingProfiles = false;

          this.actualizarEstadoControlPerfil();

          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: response => {

          this.perfiles = (response ?? []).filter(perfil =>
            perfil.estado === 1
            && !perfil.protegido
          );
        },

        error: err => {

          console.error(
            'Error al cargar perfiles disponibles:',
            err
          );

          this.perfiles = [];

          this.perfilesError = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los perfiles disponibles.'
          );
        }

      });
  }

  // ============================================================
  // CONTROLAR ESTADO DEL SELECTOR DE PERFIL
  // ============================================================

  private actualizarEstadoControlPerfil(): void {

    const control = this.usuarioForm?.get('perfilId');

    if (!control) {
      return;
    }

    if (this.perfiles.length > 0) {

      control.enable({
        emitEvent: false
      });

    } else {

      control.disable({
        emitEvent: false
      });
    }
  }

  // ============================================================
  // VERIFICAR PERFIL SELECCIONADO
  // ============================================================

  get perfilActualNoDisponible(): boolean {

    if (!this.usuarioEditar || this.perfiles.length === 0) {
      return false;
    }

    return !this.perfiles.some(
      perfil => perfil.id === this.usuarioEditar?.perfilId
    );
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
  // GUARDAR USUARIO
  // ============================================================

  guardar(): void {

    if (this.isSaving) {
      return;
    }

    if (this.isLoadingProfiles || this.perfiles.length === 0) {
      return;
    }

    if (this.usuarioForm.invalid) {

      this.usuarioForm.markAllAsTouched();

      return;
    }

    const valores = this.usuarioForm.getRawValue();

    const perfilId = Number(valores.perfilId);

    const perfilValido = this.perfiles.some(
      perfil => perfil.id === perfilId
    );

    if (!perfilValido) {

      this.errorMessage =
        'Selecciona un perfil activo y disponible para el usuario.';

      return;
    }

    const tipoDocumento = String(valores.tipoDocumento ?? '');

    if (
      tipoDocumento !== 'DNI'
      && tipoDocumento !== 'CE'
      && tipoDocumento !== 'PASAPORTE'
    ) {

      this.errorMessage = 'Selecciona un tipo de documento válido.';

      return;
    }

    const datosBase: Omit<UsuarioRequest, 'clave'> = {

      perfilId,

      tipoDocumento,

      numeroDocumento: String(
        valores.numeroDocumento ?? ''
      ).trim().toUpperCase(),

      nombres: String(
        valores.nombres ?? ''
      ).trim().replace(/\s+/g, ' '),

      apellidos: String(
        valores.apellidos ?? ''
      ).trim().replace(/\s+/g, ' '),

      username: String(
        valores.username ?? ''
      ).trim().toLowerCase(),

      correo: String(
        valores.correo ?? ''
      ).trim() || null

    };

    let peticion: Observable<Usuario>;

    if (this.usuarioEditar) {

      const datos: UsuarioUpdate = datosBase;

      peticion = this.usuarioService.actualizarUsuario(
        this.usuarioEditar.id,
        datos
      );

    } else {

      const datos: UsuarioRequest = {
        ...datosBase,
        clave: String(valores.clave ?? '')
      };

      peticion = this.usuarioService.registrarUsuario(datos);
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

        error: err => {

          console.error(
            'Error al guardar usuario:',
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
        || 'Ya existe un usuario con ese documento o nombre de acceso.';

    } else if (err?.status === 403) {

      this.errorMessage = mensaje
        || 'No tiene permisos para realizar esta operación.';

    } else if (err?.status === 0) {

      this.errorMessage =
        'No se pudo conectar con el servidor. Inténtelo nuevamente.';

    } else {

      this.errorMessage = mensaje
        || 'Ocurrió un error al guardar el usuario. Inténtelo nuevamente.';
    }

    this.cdr.detectChanges();
  }

  // ============================================================
  // MENSAJE GENERAL
  // ============================================================

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje
      ?? cuerpo?.message
      ?? (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    if (err?.status === 403) {
      return mensaje || 'No tiene permisos para consultar los perfiles.';
    }

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    return mensaje || mensajePredeterminado;
  }

}


