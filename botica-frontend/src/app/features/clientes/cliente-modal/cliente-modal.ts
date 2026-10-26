import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteService } from '../services/cliente';
import { Cliente } from '../../../core/models/cliente.model';

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
  isLoading: boolean = false;
  errorMessage: string = '';
  modoRegistro: 'AUTO' | 'MANUAL' = 'AUTO';
  esEdicionAuto: boolean = false;

  private fb = inject(FormBuilder);
  private clienteService = inject(ClienteService);

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario() {
    this.clienteForm = this.fb.group({
      tipoDocumento: ['DNI', Validators.required],
      numeroDocumento: ['', [Validators.required, Validators.pattern('^\\d{8}$')]],
      nombres: ['', [Validators.pattern('^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$')]],
      apellidos: ['', [Validators.pattern('^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$')]],
      correo: ['', [Validators.email]]
    });

    this.cambiarModo('AUTO');

    this.clienteForm.get('tipoDocumento')?.valueChanges.subscribe(tipo => {
      this.actualizarValidacionDocumento(tipo);
    });

    if (this.clienteEditar) {
      this.clienteForm.patchValue({
        tipoDocumento: this.clienteEditar.tipoDocumento,
        numeroDocumento: this.clienteEditar.numeroDocumento,
        nombres: this.clienteEditar.nombres,
        apellidos: this.clienteEditar.apellidos,
        correo: this.clienteEditar.correo
      });
      
      this.clienteForm.get('tipoDocumento')?.disable();
      this.clienteForm.get('numeroDocumento')?.disable();

      // Usamos el campo real de la BD
      this.esEdicionAuto = (this.clienteEditar.origenDatos === 'RENIEC');
      
      if (this.esEdicionAuto) {
        this.modoRegistro = 'AUTO';
        this.cambiarModo('AUTO');
      } else {
        this.modoRegistro = 'MANUAL';
        this.cambiarModo('MANUAL');
      }
    }
  }

  onInputDocumento(event: Event) {
    const input = event.target as HTMLInputElement;
    const tipo = this.clienteForm.get('tipoDocumento')?.value;
    let valor = input.value;

    if (tipo === 'DNI') {
      valor = valor.replace(/[^0-9]/g, ''); 
      valor = valor.substring(0, 8);        
    } else if (tipo === 'CE') {
      valor = valor.replace(/[^a-zA-Z0-9]/g, ''); 
      valor = valor.substring(0, 11);             
    } else if (tipo === 'PASAPORTE') {
      valor = valor.replace(/[^a-zA-Z0-9]/g, '');
      valor = valor.substring(0, 15);
    }

    if (input.value !== valor) {
      input.value = valor;
      this.clienteForm.get('numeroDocumento')?.setValue(valor);
    }
  }

  onInputNombre(event: Event, controlName: string) {
    const input = event.target as HTMLInputElement;
    const valor = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
    if (input.value !== valor) {
      input.value = valor;
      this.clienteForm.get(controlName)?.setValue(valor);
    }
  }

  cambiarModo(modo: 'AUTO' | 'MANUAL') {
    this.modoRegistro = modo;
    const nombresCtrl = this.clienteForm.get('nombres');
    const apellidosCtrl = this.clienteForm.get('apellidos');

    if (modo === 'AUTO' || this.esEdicionAuto) {
      nombresCtrl?.clearValidators();
      apellidosCtrl?.clearValidators();
      nombresCtrl?.disable();
      apellidosCtrl?.disable();
    } else {
      nombresCtrl?.setValidators([Validators.required, Validators.pattern('^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$')]);
      apellidosCtrl?.setValidators([Validators.required, Validators.pattern('^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$')]);
      nombresCtrl?.enable();
      apellidosCtrl?.enable();
    }
    nombresCtrl?.updateValueAndValidity();
    apellidosCtrl?.updateValueAndValidity();
  }

  actualizarValidacionDocumento(tipo: string) {
    const docCtrl = this.clienteForm.get('numeroDocumento');
    if (tipo === 'DNI') {
      docCtrl?.setValidators([Validators.required, Validators.pattern('^\\d{8}$')]);
    } else if (tipo === 'CE') {
      docCtrl?.setValidators([Validators.required, Validators.pattern('^[a-zA-Z0-9]{9,11}$')]);
    } else {
      docCtrl?.setValidators([Validators.required, Validators.pattern('^[a-zA-Z0-9]{9,15}$')]);
    }
    docCtrl?.updateValueAndValidity();
  }

  guardar() {
    if (this.clienteForm.invalid) {
      this.clienteForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    const formData = this.clienteForm.getRawValue();

    // ASIGNAR ORIGEN DE DATOS
    if (!this.clienteEditar) {
      formData.origenDatos = (this.modoRegistro === 'AUTO') ? 'RENIEC' : 'MANUAL';
    } else {
      formData.origenDatos = this.clienteEditar.origenDatos;
    }

    if (this.clienteEditar) {
      this.clienteService.actualizarCliente(this.clienteEditar.id, formData).subscribe({
        next: () => this.finalizarGuardado(),
        error: (err) => this.manejarError(err)
      });
    } else {
      this.clienteService.registrarCliente(formData).subscribe({
        next: () => this.finalizarGuardado(),
        error: (err) => this.manejarError(err)
      });
    }
  }

  finalizarGuardado() {
    this.isLoading = false;
    this.guardado.emit();
    this.cerrar.emit();
  }

  manejarError(err: any) {
    this.isLoading = false;
    this.errorMessage = err.error?.mensaje || 'Ocurrió un error al guardar el cliente.';
  }
}


