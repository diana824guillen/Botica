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

import { Observable, finalize } from 'rxjs';

import {
  Producto,
  Presentacion,
  PresentacionRequest,
  TipoPresentacion
} from '../../../core/models/producto.model';

import { ProductoService } from '../services/producto';

type AccionPresentacion = 'activar' | 'desactivar' | 'eliminar';

interface ConfirmacionPresentacion {
  tipo: AccionPresentacion;
  presentacion: Presentacion;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-presentaciones-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './presentaciones-modal.html',
  styleUrls: ['./presentaciones-modal.scss']
})
export class PresentacionesModalComponent implements OnInit {

  @Input({ required: true }) producto!: Producto;

  @Output() cerrar = new EventEmitter<void>();
  @Output() actualizado = new EventEmitter<void>();

  presentaciones: Presentacion[] = [];

  presentacionForm!: FormGroup;
  presentacionEditar: Presentacion | null = null;

  isLoading = false;
  isSaving = false;
  procesandoAccion = false;

  errorMessage = '';
  errorConfirmacion = '';

  confirmacion: ConfirmacionPresentacion | null = null;

  tiposPresentacion: { value: TipoPresentacion; label: string }[] = [
    { value: 'UNIDAD', label: 'Unidad' },
    { value: 'BLISTER', label: 'Blíster' },
    { value: 'CAJA', label: 'Caja' },
    { value: 'FRASCO', label: 'Frasco' },
    { value: 'TIRA', label: 'Tira' },
    { value: 'OTRO', label: 'Otro' }
  ];

  private fb = inject(FormBuilder);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {

    this.presentacionForm = this.fb.group({

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

    this.cargarPresentaciones();
  }

  cargarPresentaciones(): void {

    this.isLoading = true;
    this.errorMessage = '';

    this.productoService
      .listarPresentaciones(this.producto.id)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.presentaciones = respuesta || [];
        },

        error: (err) => {

          console.error('Error al cargar presentaciones:', err);

          this.errorMessage = this.obtenerMensajeError(
            err,
            'No se pudieron cargar las presentaciones.'
          );
        }

      });
  }

  etiquetaTipo(tipo: TipoPresentacion): string {

    return this.tiposPresentacion.find(
      opcion => opcion.value === tipo
    )?.label || tipo;
  }

  editarPresentacion(presentacion: Presentacion): void {

    this.presentacionEditar = presentacion;
    this.errorMessage = '';

    this.presentacionForm.patchValue({
      tipo: presentacion.tipo,
      factorConversion: presentacion.factorConversion,
      precioVenta: presentacion.precioVenta
    });
  }

  cancelarEdicion(): void {

    this.presentacionEditar = null;
    this.errorMessage = '';

    this.presentacionForm.reset({
      tipo: 'UNIDAD',
      factorConversion: 1,
      precioVenta: null
    });
  }

  guardarPresentacion(): void {

    if (this.isSaving) {
      return;
    }

    if (this.presentacionForm.invalid) {
      this.presentacionForm.markAllAsTouched();
      return;
    }

    const valores = this.presentacionForm.getRawValue();

    const solicitud: PresentacionRequest = {
      tipo: valores.tipo as TipoPresentacion,
      factorConversion: Number(valores.factorConversion),
      precioVenta: Number(valores.precioVenta)
    };

    // Comprobar que no exista otra presentación con el mismo tipo
    // y factor, excluyendo el registro que se está editando.
    const duplicada = this.presentaciones.some(p =>
      p.tipo === solicitud.tipo &&
      p.factorConversion === solicitud.factorConversion &&
      p.estado === 1 &&
      p.id !== this.presentacionEditar?.id
    );

    if (duplicada) {

      this.errorMessage =
        'Ya existe una presentación activa con ese tipo y factor de conversión.';

      return;
    }

    this.errorMessage = '';
    this.isSaving = true;

    const peticion: Observable<unknown> = this.presentacionEditar

      ? this.productoService.actualizarPresentacion(
          this.producto.id,
          this.presentacionEditar.id,
          solicitud
        )

      : this.productoService.agregarPresentaciones(
          this.producto.id,
          [solicitud]
        );

    peticion
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {

          this.cancelarEdicion();
          this.cargarPresentaciones();
          this.actualizado.emit();
        },

        error: (err) => {
          this.errorMessage = this.obtenerMensajeError(
            err,
            'No se pudo guardar la presentación.'
          );
        }

      });
  }

  solicitarCambioEstado(presentacion: Presentacion): void {

    const activar = presentacion.estado === 0;

    this.confirmacion = {

      tipo: activar ? 'activar' : 'desactivar',

      presentacion,

      titulo: activar
        ? 'Activar presentación'
        : 'Desactivar presentación',

      mensaje: activar
        ? '¿Desea volver a activar esta presentación?'
        : '¿Desea desactivar esta presentación?',

      textoBoton: activar ? 'Activar' : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  solicitarEliminacion(presentacion: Presentacion): void {

    this.confirmacion = {

      tipo: 'eliminar',

      presentacion,

      titulo: 'Eliminar presentación',

      mensaje:
        'La presentación dejará de utilizarse en el sistema. ¿Desea continuar?',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  cerrarConfirmacion(): void {

    if (this.procesandoAccion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoAccion) {
      return;
    }

    this.procesandoAccion = true;
    this.errorConfirmacion = '';

    const idPresentacion = accion.presentacion.id;

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'

        ? this.productoService.eliminarPresentacion(
            this.producto.id,
            idPresentacion
          )

        : this.productoService.cambiarEstadoPresentacion(
            this.producto.id,
            idPresentacion,
            accion.tipo === 'activar' ? 1 : 0
          );

    peticion
      .pipe(
        finalize(() => {
          this.procesandoAccion = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {

          this.confirmacion = null;

          if (this.presentacionEditar?.id === idPresentacion) {
            this.cancelarEdicion();
          }

          this.cargarPresentaciones();
          this.actualizado.emit();
        },

        error: (err) => {

          this.errorConfirmacion = this.obtenerMensajeError(
            err,
            'No se pudo completar la operación.'
          );
        }

      });
  }

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {

    const cuerpo = err?.error;

    const mensaje = String(
      cuerpo?.mensaje ??
      cuerpo?.message ??
      (typeof cuerpo === 'string' ? cuerpo : '')
    ).trim();

    if (err?.status === 403) {
      return mensaje || 'No tiene permisos para realizar esta operación.';
    }

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    return mensaje || mensajePredeterminado;
  }
}


