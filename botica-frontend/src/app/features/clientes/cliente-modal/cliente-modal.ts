import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ChangeDetectorRef,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import { ClienteService } from '../services/cliente';
import { Cliente } from '../../../core/models/cliente.model';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-cliente-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cliente-modal.html',
  styleUrls: ['./cliente-modal.scss']
})
export class ClienteModalComponent implements OnInit {

  @Input() clienteEditar: Cliente | null = null;

  @Output() cerrar = new EventEmitter<void>();
  @Output() guardado = new EventEmitter<void>();

  clienteForm!: FormGroup;

  isLoading = false;
  errorMessage = '';
  mostrarAlternativaManual = false;

  modoRegistro: 'AUTO' | 'MANUAL' = 'AUTO';
  esEdicionAuto = false;

  private fb = inject(FormBuilder);
  private clienteService = inject(ClienteService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {

    this.clienteForm = this.fb.group({
      tipoDocumento: ['DNI', Validators.required],

      numeroDocumento: [
        '',
        [
          Validators.required,
          Validators.pattern(/^\d{8}$/)
        ]
      ],

      nombres: [
        '',
        [
          Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
        ]
      ],

      apellidos: [
        '',
        [
          Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
        ]
      ],

      correo: ['', [Validators.email]]
    });

    this.cambiarModo('AUTO');

    this.clienteForm.get('tipoDocumento') ?.valueChanges.subscribe(tipo => {
      this.actualizarValidacionDocumento(tipo);

      if (!this.clienteEditar) {
        this.clienteForm.get('numeroDocumento') ?.reset('', { emitEvent: false });
      }

      // RENIEC solo se utiliza para documentos DNI.
      if (!this.clienteEditar && tipo !== 'DNI' && this.modoRegistro === 'AUTO') {
        this.cambiarModo('MANUAL');
      }
    });

    // Configurar el formulario cuando se edita un cliente.
    if (this.clienteEditar) {

      this.clienteForm.patchValue({
        tipoDocumento: this.clienteEditar.tipoDocumento,
        numeroDocumento: this.clienteEditar.numeroDocumento,
        nombres: this.clienteEditar.nombres,
        apellidos: this.clienteEditar.apellidos,
        correo: this.clienteEditar.correo
      });

      // No permitir modificar el tipo ni el número de documento.
      this.clienteForm.get('tipoDocumento')?.disable();
      this.clienteForm.get('numeroDocumento')?.disable();

      this.esEdicionAuto =
        this.clienteEditar.origenDatos === 'RENIEC';

      this.cambiarModo(
        this.esEdicionAuto ? 'AUTO' : 'MANUAL'
      );
    }
  }

  onInputDocumento(event: Event): void {

    const input = event.target as HTMLInputElement;
    const tipo = this.clienteForm.get('tipoDocumento')?.value;

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

      this.clienteForm
        .get('numeroDocumento')
        ?.setValue(valor);
    }
  }

  onInputNombre(event: Event, controlName: string): void {

    const input = event.target as HTMLInputElement;

    const valor = input.value.replace(
      /[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g,
      ''
    );

    if (input.value !== valor) {
      input.value = valor;

      this.clienteForm
        .get(controlName)
        ?.setValue(valor);
    }
  }

  cambiarModo(modo: 'AUTO' | 'MANUAL'): void {
    const modoAnterior = this.modoRegistro;
    this.modoRegistro = modo;

    // Limpiar mensajes anteriores al cambiar de modalidad.
    this.errorMessage = '';
    this.mostrarAlternativaManual = false;

    const nombresCtrl = this.clienteForm.get('nombres');
    const apellidosCtrl = this.clienteForm.get('apellidos');
    const documentoCtrl = this.clienteForm.get('numeroDocumento');

    // Limpiar el número de documento
    if (
      !this.clienteEditar &&
      modoAnterior !== modo
    ) {
      documentoCtrl?.reset('', { emitEvent: false });
    }

    // limpiar los nombres y apellidos ingresados.
    if (
      !this.clienteEditar &&
      modoAnterior === 'MANUAL' &&
      modo === 'AUTO'
    ) {
      nombresCtrl?.reset('', { emitEvent: false });
      apellidosCtrl?.reset('', { emitEvent: false });
    }

    if (modo === 'AUTO' || this.esEdicionAuto) {

      nombresCtrl?.clearValidators();
      apellidosCtrl?.clearValidators();

      nombresCtrl?.disable();
      apellidosCtrl?.disable();

    } else {

      nombresCtrl?.setValidators([
        Validators.required,
        Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
      ]);

      apellidosCtrl?.setValidators([
        Validators.required,
        Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
      ]);

      nombresCtrl?.enable();
      apellidosCtrl?.enable();
    }

    nombresCtrl?.updateValueAndValidity();
    apellidosCtrl?.updateValueAndValidity();
  }

  actualizarValidacionDocumento(tipo: string): void {

    const docCtrl = this.clienteForm.get('numeroDocumento');

    if (tipo === 'DNI') {

      docCtrl?.setValidators([
        Validators.required,
        Validators.pattern(/^\d{8}$/)
      ]);

    } else if (tipo === 'CE') {

      docCtrl?.setValidators([
        Validators.required,
        Validators.pattern(/^[a-zA-Z0-9]{9,11}$/)
      ]);

    } else {

      docCtrl?.setValidators([
        Validators.required,
        Validators.pattern(/^[a-zA-Z0-9]{9,15}$/)
      ]);
    }

    docCtrl?.updateValueAndValidity();
  }

  guardar(): void {

    if (this.clienteForm.invalid) {
      this.clienteForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.mostrarAlternativaManual = false;

    const formData = this.clienteForm.getRawValue();

    if (!this.clienteEditar) {

      formData.origenDatos =
        this.modoRegistro === 'AUTO' ? 'RENIEC' : 'MANUAL';

    } else {

      formData.origenDatos = this.clienteEditar.origenDatos;
    }

    const peticion = this.clienteEditar
      ? this.clienteService.actualizarCliente(
          this.clienteEditar.id,
          formData
        )
      : this.clienteService.registrarCliente(formData);

    peticion
      .pipe(
        finalize(() => {
          // Se ejecuta tanto si la petición funciona
          // como si termina con un error.
          this.isLoading = false;
        })
      )
      .subscribe({

        next: () => {
          this.finalizarGuardado();
        },

        error: (err) => {
          console.error('Error al guardar cliente:', err);
          this.manejarError(err);
        }

      });
  }

  finalizarGuardado(): void {

    this.isLoading = false;

    this.guardado.emit();
    this.cerrar.emit();
  }

  manejarError(err: any): void {

    this.isLoading = false;
    this.mostrarAlternativaManual = false;

    const cuerpoError = err?.error;

    const mensaje = String(
      cuerpoError?.mensaje ??
      cuerpoError?.message ??
      (typeof cuerpoError === 'string' ? cuerpoError : '')
    ).trim();

    const mensajeMin = mensaje.toLowerCase();

    // CASO 1: el documento ya está registrado.
    if (
      err?.status === 409 ||
      mensajeMin.includes('ya existe un cliente') ||
      mensajeMin.includes('documento duplicado') ||
      mensajeMin.includes('uq_cliente_documento')
    ) {

      this.errorMessage =
        'Este número de documento ya está registrado. Ingrese un documento diferente.';

    } else {

      // CASO 2: falla la consulta automática a RENIEC.
      const errorReniec =
        this.modoRegistro === 'AUTO' &&
        (
          mensajeMin.includes('miapi') ||
          mensajeMin.includes('reniec') ||
          mensajeMin.includes('cuota') ||
          mensajeMin.includes('no está disponible') ||
          mensajeMin.includes('no esta disponible')
        );

      if (errorReniec) {

        this.errorMessage = mensaje ||
          'No fue posible consultar RENIEC. Puede registrar al cliente manualmente.';

        this.mostrarAlternativaManual = true;

      } else if (mensaje) {

        // CASO 3: el backend envió otro mensaje.
        this.errorMessage = mensaje;

      } else if (err?.status === 0) {

        this.errorMessage =
          'No se pudo conectar con el servidor. Verifique su conexión e inténtelo nuevamente.';

      } else {

        // CASO 4: error no identificado.
        this.errorMessage =
          'Ocurrió un error al guardar el cliente. Inténtelo nuevamente.';
      }
    }

    // Actualiza la vista para mostrar la alerta y habilitar los botones.
    this.cdr.detectChanges();
  }
}


