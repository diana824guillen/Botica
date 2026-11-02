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
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import {
  Descuento,
  DescuentoRequest,
  DescuentoUpdateRequest,
  TipoCondicionDescuento
} from '../../../core/models/descuento.model';

import { Producto } from '../../../core/models/producto.model';

import { DescuentoService } from '../services/descuento';
import { ProductoService } from '../../productos/services/producto';

function validarRangoFechas(
  control: AbstractControl
): ValidationErrors | null {

  const inicio = control.get('fechaInicio')?.value as string | null;
  const fin = control.get('fechaFin')?.value as string | null;

  if (inicio && fin && fin < inicio) {
    return { rangoFechasInvalido: true };
  }

  return null;
}

@Component({
  selector: 'app-descuento-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './descuento-modal.html',
  styleUrls: ['./descuento-modal.scss']
})
export class DescuentoModalComponent implements OnInit {

  @Input() descuentoEditar: Descuento | null = null;

  @Output() cerrar = new EventEmitter<void>();
  @Output() guardado = new EventEmitter<void>();

  descuentoForm!: FormGroup;

  productos: Producto[] = [];

  isSaving = false;
  cargandoProductos = false;
  errorMessage = '';

  tiposCondicion: {
    value: TipoCondicionDescuento;
    label: string;
  }[] = [
    {
      value: 'MONTO_MINIMO',
      label: 'Monto mínimo de compra'
    },
    {
      value: 'CANTIDAD_MINIMA',
      label: 'Cantidad mínima de unidades'
    }
  ];

  private fb = inject(FormBuilder);
  private descuentoService = inject(DescuentoService);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.inicializarFormulario();

    if (!this.descuentoEditar) {
      this.cargarProductos();
    }
  }

  // ============================================================
  // INICIALIZAR FORMULARIO
  // ============================================================

  inicializarFormulario(): void {

    this.descuentoForm = this.fb.group(
      {
        idProducto: [
          null,
          this.descuentoEditar
            ? []
            : [Validators.required]
        ],

        nombre: [
          '',
          [
            Validators.required,
            Validators.maxLength(100),
            Validators.pattern(/\S/)
          ]
        ],

        tipoCondicion: [
          null,
          Validators.required
        ],

        valorCondicion: [
          null,
          [
            Validators.required,
            Validators.min(0.01),
            Validators.max(99999999.99),
            Validators.pattern(/^\d+(\.\d{1,2})?$/)
          ]
        ],

        porcentaje: [
          null,
          [
            Validators.required,
            Validators.min(0.01),
            Validators.max(100),
            Validators.pattern(/^\d+(\.\d{1,2})?$/)
          ]
        ],

        fechaInicio: [null],

        fechaFin: [null]
      },
      {
        validators: [validarRangoFechas]
      }
    );

    if (this.descuentoEditar) {

      this.descuentoForm.patchValue({
        nombre: this.descuentoEditar.nombre,
        tipoCondicion: this.descuentoEditar.tipoCondicion,
        valorCondicion: this.descuentoEditar.valorCondicion,
        porcentaje: this.descuentoEditar.porcentaje,
        fechaInicio: this.descuentoEditar.fechaInicio,
        fechaFin: this.descuentoEditar.fechaFin
      });

    }
  }

  // ============================================================
  // CARGAR PRODUCTOS PARA CREAR UNA PROMOCIÓN
  // ============================================================

  cargarProductos(): void {

    this.cargandoProductos = true;
    this.errorMessage = '';

    this.productoService
      .listarProductos()
      .pipe(
        finalize(() => {
          this.cargandoProductos = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {

          this.productos = (respuesta || [])
            .filter(producto => producto.estado === 1)
            .sort((a, b) =>
              a.nombre.localeCompare(b.nombre, 'es')
            );

          if (this.productos.length === 0) {
            this.errorMessage =
              'No hay productos activos disponibles para crear una promoción.';
          }
        },

        error: (err) => {

          console.error('Error al cargar productos:', err);

          this.productos = [];

          this.errorMessage = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los productos. Inténtelo nuevamente.'
          );
        }

      });
  }

  // ============================================================
  // GUARDAR
  // ============================================================

  guardar(): void {

    if (this.isSaving || this.cargandoProductos) {
      return;
    }

    if (this.descuentoForm.invalid) {
      this.descuentoForm.markAllAsTouched();
      return;
    }

    const valores = this.descuentoForm.getRawValue();

    const nombre = String(valores.nombre ?? '')
      .trim()
      .replace(/\s+/g, ' ');

    if (!nombre) {

      this.descuentoForm.get('nombre')?.setErrors({
        required: true
      });

      this.descuentoForm.markAllAsTouched();
      return;
    }

    const valorCondicion = Number(valores.valorCondicion);
    const porcentaje = Number(valores.porcentaje);

    if (
      !Number.isFinite(valorCondicion)
      || valorCondicion < 0.01
      || valorCondicion > 99999999.99
    ) {

      this.descuentoForm.get('valorCondicion')?.setErrors({
        min: valorCondicion < 0.01,
        max: valorCondicion > 99999999.99
      });

      return;
    }

    if (
      !Number.isFinite(porcentaje)
      || porcentaje < 0.01
      || porcentaje > 100
    ) {

      this.descuentoForm.get('porcentaje')?.setErrors({
        min: porcentaje < 0.01,
        max: porcentaje > 100
      });

      return;
    }

    const fechaInicio = valores.fechaInicio || null;
    const fechaFin = valores.fechaFin || null;

    if (
      fechaInicio
      && fechaFin
      && fechaFin < fechaInicio
    ) {

      this.descuentoForm.markAllAsTouched();

      this.descuentoForm.setErrors({
        rangoFechasInvalido: true
      });

      return;
    }

    let peticion: Observable<Descuento>;

    if (this.descuentoEditar) {

      const datos: DescuentoUpdateRequest = {
        nombre,
        tipoCondicion: valores.tipoCondicion,
        valorCondicion,
        porcentaje,
        fechaInicio,
        fechaFin
      };

      peticion = this.descuentoService.actualizarDescuento(
        this.descuentoEditar.id,
        datos
      );

    } else {

      const idProducto = Number(valores.idProducto);

      if (
        !Number.isInteger(idProducto)
        || idProducto <= 0
      ) {

        this.descuentoForm.get('idProducto')?.setErrors({
          required: true
        });

        return;
      }

      const datos: DescuentoRequest = {
        idProducto,
        nombre,
        tipoCondicion: valores.tipoCondicion,
        valorCondicion,
        porcentaje,
        fechaInicio,
        fechaFin
      };

      peticion = this.descuentoService.crearDescuento(datos);
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

  // ============================================================
  // MENSAJES DE ERROR
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
      return mensaje
        || 'No tiene permisos para realizar esta operación.';
    }

    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor. Inténtelo nuevamente.';
    }

    return mensaje || mensajePredeterminado;
  }

  manejarError(err: any): void {

    console.error('Error al guardar la promoción:', err);

    this.errorMessage = this.obtenerMensajeError(
      err,
      'Ocurrió un error al guardar la promoción. Inténtelo nuevamente.'
    );

    this.cdr.detectChanges();
  }
}


