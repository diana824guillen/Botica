import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';

import { Observable, finalize } from 'rxjs';

import {
  Lote,
  LoteRequest,
  LoteAjusteRequest,
  MotivoAjuste
} from '../../../core/models/lote.model';

import {
  Producto,
  Presentacion,
  TipoPresentacion
} from '../../../core/models/producto.model';

import { HasPermissionDirective } from '../../../core/directives/has-permission';

import { ProductoService } from '../../productos/services/producto';
import { LoteService } from '../services/lote';

type TipoConfirmacionLote =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

type FiltroEstadoLote = 'todos' | '1' | '0';

type FiltroVencimiento =
  | 'todos'
  | 'por-vencer'
  | 'vencidos';

interface ConfirmacionLote {
  id: number;
  tipo: TipoConfirmacionLote;
  numeroLote: string;
  nombreProducto: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

interface MotivoAjusteOpcion {
  value: MotivoAjuste;
  label: string;
  tipoMovimiento: 'MERMA' | 'AJUSTE';
}

@Component({
  selector: 'app-lotes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HasPermissionDirective
  ],
  templateUrl: './lotes-list.html',
  styleUrls: ['./lotes-list.scss']
})
export class LotesListComponent implements OnInit {

  // ============================================================
  // DATOS
  // ============================================================

  lotes: Lote[] = [];
  productos: Producto[] = [];

  isLoading = true;
  isLoadingProductos = false;
  isSaving = false;

  errorCarga = '';
  errorProductos = '';
  errorModal = '';

  // ============================================================
  // MODALES
  // ============================================================

  mostrarModalCrear = false;
  mostrarModalAjuste = false;

  loteSeleccionado: Lote | null = null;

  productoIngresoSeleccionado: Producto | null = null;
  productoAjusteSeleccionado: Producto | null = null;

  loteForm!: FormGroup;
  ajusteForm!: FormGroup;

  // ============================================================
  // BÚSQUEDA Y FILTROS
  // ============================================================

  searchTerm = '';

  filtroEstado: FiltroEstadoLote = 'todos';
  filtroVencimiento: FiltroVencimiento = 'todos';

  currentPage = 0;
  pageSize = 10;

  // ============================================================
  // CONFIRMACIONES
  // ============================================================

  confirmacion: ConfirmacionLote | null = null;

  procesandoConfirmacion = false;
  errorConfirmacion = '';

  // ============================================================
  // MOTIVOS DE AJUSTE
  // ============================================================

  motivosAjuste: MotivoAjusteOpcion[] = [
    {
      value: 'PRODUCTO_VENCIDO',
      label: 'Producto vencido',
      tipoMovimiento: 'MERMA'
    },
    {
      value: 'PRODUCTO_DAÑADO',
      label: 'Producto dañado o roto',
      tipoMovimiento: 'MERMA'
    },
    {
      value: 'PERDIDA_ROBO',
      label: 'Pérdida por robo',
      tipoMovimiento: 'MERMA'
    },
    {
      value: 'DEVOLUCION_PROVEEDOR',
      label: 'Devolución a proveedor',
      tipoMovimiento: 'MERMA'
    },
    {
      value: 'USO_INTERNO_DONACION',
      label: 'Uso interno o donación',
      tipoMovimiento: 'MERMA'
    },
    {
      value: 'ERROR_CONTEO_FISICO',
      label: 'Faltante o sobrante en conteo',
      tipoMovimiento: 'AJUSTE'
    },
    {
      value: 'ERROR_DIGITACION',
      label: 'Error de digitación',
      tipoMovimiento: 'AJUSTE'
    },
    {
      value: 'AJUSTE_EMPAQUE',
      label: 'Ajuste por apertura de empaque',
      tipoMovimiento: 'AJUSTE'
    }
  ];

  // ============================================================
  // SERVICIOS
  // ============================================================

  private fb = inject(FormBuilder);
  private loteService = inject(LoteService);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  // ============================================================
  // INICIALIZACIÓN
  // ============================================================

  ngOnInit(): void {

    this.inicializarFormularioLote();
    this.inicializarFormularioAjuste();

    this.cargarLotes();
    this.cargarProductos();
  }

  private inicializarFormularioLote(): void {

    this.loteForm = this.fb.group({
      idProducto: [
        null,
        Validators.required
      ],

      numeroLote: [
        '',
        [
          Validators.required,
          Validators.maxLength(50)
        ]
      ],

      fechaVencimiento: [
        '',
        [
          Validators.required,
          this.validarFechaFutura.bind(this)
        ]
      ],

      detallesIngreso: this.fb.array([])
    });
  }

  private inicializarFormularioAjuste(): void {

    this.ajusteForm = this.fb.group({
      idPresentacion: [
        null,
        Validators.required
      ],

      cantidadAjuste: [
        null,
        [
          Validators.required,
          Validators.pattern(/^-?\d+$/)
        ]
      ],

      motivo: [
        'ERROR_CONTEO_FISICO',
        Validators.required
      ],

      observacion: [
        '',
        Validators.maxLength(200)
      ]
    });
  }

  // ============================================================
  // VALIDACIÓN DE FECHA FUTURA
  // ============================================================

  private validarFechaFutura(
    control: AbstractControl
  ): ValidationErrors | null {

    const fecha = String(control.value || '').trim();

    if (!fecha) {
      return null;
    }

    // Las fechas YYYY-MM-DD pueden compararse directamente
    // sin convertirlas a UTC ni provocar cambios de día.
    return fecha > this.fechaHoyISO()
      ? null
      : { fechaNoFutura: true };
  }

  get fechaMinimaVencimiento(): string {

    const fecha = new Date();

    fecha.setDate(fecha.getDate() + 1);

    return this.formatoFechaLocal(fecha);
  }

  // ============================================================
  // FORMULARIO DE INGRESO
  // ============================================================

  get detallesIngresoArray(): FormArray {
    return this.loteForm.get('detallesIngreso') as FormArray;
  }

  get productosParaIngreso(): Producto[] {

    return this.productos.filter(producto =>
      producto.estado === 1 &&
      (producto.presentaciones || []).some(
        presentacion => presentacion.estado === 1
      )
    );
  }

  get presentacionesParaAjuste(): Presentacion[] {

    return (
      this.productoAjusteSeleccionado?.presentaciones || []
    ).filter(presentacion => presentacion.estado === 1);
  }

  abrirModalCrear(): void {

    if (this.isSaving) {
      return;
    }

    this.errorModal = '';
    this.productoIngresoSeleccionado = null;

    this.loteForm.reset({
      idProducto: null,
      numeroLote: '',
      fechaVencimiento: ''
    });

    this.detallesIngresoArray.clear();

    this.mostrarModalCrear = true;
  }

  cerrarModalCrear(): void {

    if (this.isSaving) {
      return;
    }

    this.mostrarModalCrear = false;
    this.productoIngresoSeleccionado = null;
    this.errorModal = '';
  }

  cambiarProductoSeleccionado(): void {

    const valorId = this.loteForm.get('idProducto')?.value;

    const idProducto = valorId == null
      ? null
      : Number(valorId);

    this.productoIngresoSeleccionado =
      idProducto == null
        ? null
        : this.productos.find(
            producto => producto.id === idProducto
          ) || null;

    this.detallesIngresoArray.clear();
    this.errorModal = '';

    if (!this.productoIngresoSeleccionado) {
      return;
    }

    const presentacionesActivas = (
      this.productoIngresoSeleccionado.presentaciones || []
    ).filter(presentacion => presentacion.estado === 1);

    presentacionesActivas.forEach(presentacion => {

      this.detallesIngresoArray.push(
        this.fb.group({
          idPresentacion: [
            presentacion.id
          ],

          nombrePresentacion: [
            this.etiquetaTipo(presentacion.tipo)
          ],

          factorConversion: [
            presentacion.factorConversion
          ],

          cantidadPresentacion: [
            0,
            [
              Validators.required,
              Validators.min(0),
              Validators.pattern(/^\d+$/)
            ]
          ]
        })
      );
    });
  }

  guardarLote(): void {

    if (this.isSaving) {
      return;
    }

    if (this.loteForm.invalid) {
      this.loteForm.markAllAsTouched();
      return;
    }

    const valores = this.loteForm.getRawValue();

    const detallesFormulario =
      this.detallesIngresoArray.getRawValue();

    // Cada cantidad recibida debe ser un entero no negativo.
    const cantidadInvalida = detallesFormulario.some(
      (detalle: any) => {

        const cantidad = Number(
          detalle.cantidadPresentacion
        );

        return !Number.isFinite(cantidad) ||
          !Number.isInteger(cantidad) ||
          cantidad < 0;
      }
    );

    if (cantidadInvalida) {
      this.errorModal =
        'Las cantidades recibidas deben ser números enteros iguales o mayores que cero.';
      return;
    }

    // No se envían presentaciones cuya cantidad sea cero.
    const detallesIngreso = detallesFormulario
      .map((detalle: any) => ({
        idPresentacion: Number(detalle.idPresentacion),
        cantidadPresentacion: Number(
          detalle.cantidadPresentacion
        )
      }))
      .filter((detalle: any) =>
        detalle.cantidadPresentacion > 0
      );

    if (detallesIngreso.length === 0) {
      this.errorModal =
        'Ingrese una cantidad mayor que cero en al menos una presentación.';
      return;
    }

    const fechaVencimiento = String(
      valores.fechaVencimiento || ''
    );

    // Validación adicional al control del formulario.
    if (
      !fechaVencimiento ||
      fechaVencimiento <= this.fechaHoyISO()
    ) {
      this.errorModal =
        'La fecha de vencimiento debe ser posterior a hoy.';
      return;
    }

    const request: LoteRequest = {
      numeroLote: String(
        valores.numeroLote || ''
      ).trim().replace(/\s+/g, ' '),

      fechaVencimiento,

      detallesIngreso
    };

    this.isSaving = true;
    this.errorModal = '';

    this.loteService
      .crearLote(Number(valores.idProducto), request)
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {

          this.mostrarModalCrear = false;
          this.productoIngresoSeleccionado = null;

          this.cargarLotes();
        },

        error: (err) => {

          this.errorModal = this.obtenerMensajeError(
            err,
            'No se pudo registrar el lote.'
          );
        }

      });
  }

  // ============================================================
  // CARGAR PRODUCTOS
  // ============================================================

  cargarProductos(): void {

    this.isLoadingProductos = true;
    this.errorProductos = '';

    this.productoService
      .listarProductos()
      .pipe(
        finalize(() => {
          this.isLoadingProductos = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {
          this.productos = respuesta || [];
        },

        error: (err) => {

          this.errorProductos = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los productos.'
          );
        }

      });
  }

  // ============================================================
  // CARGAR LOTES
  // ============================================================

  cargarLotes(): void {

    this.isLoading = true;
    this.errorCarga = '';

    this.loteService
      .listarLotes()
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.ajustarPagina();
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (respuesta) => {

          this.lotes = respuesta || [];

          this.ajustarPagina();
        },

        error: (err) => {

          this.lotes = [];

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los lotes.'
          );
        }

      });
  }

  // ============================================================
  // BÚSQUEDA Y FILTROS
  // ============================================================

  get lotesFiltrados(): Lote[] {

    const termino = this.searchTerm
      .trim()
      .toLowerCase();

    return this.lotes.filter(lote => {

      const nombreProducto = (
        lote.nombreProducto || ''
      ).toLowerCase();

      const numeroLote = (
        lote.numeroLote || ''
      ).toLowerCase();

      const coincideTexto =
        !termino ||
        nombreProducto.includes(termino) ||
        numeroLote.includes(termino);

      const coincideEstado =
        this.filtroEstado === 'todos' ||
        String(lote.estado) === this.filtroEstado;

      let coincideVencimiento = true;

      if (this.filtroVencimiento === 'vencidos') {

        coincideVencimiento = this.esVencido(lote);

      } else if (
        this.filtroVencimiento === 'por-vencer'
      ) {

        coincideVencimiento = this.estaPorVencer(lote);
      }

      return coincideTexto &&
        coincideEstado &&
        coincideVencimiento;
    });
  }

  get totalPages(): number {

    return Math.ceil(
      this.lotesFiltrados.length / this.pageSize
    );
  }

  get lotesPaginados(): Lote[] {

    const inicio = this.currentPage * this.pageSize;

    return this.lotesFiltrados.slice(
      inicio,
      inicio + this.pageSize
    );
  }

  onSearchInput(event: Event): void {

    this.searchTerm = (
      event.target as HTMLInputElement
    ).value;

    this.currentPage = 0;
  }

  cambiarFiltroEstado(
    estado: FiltroEstadoLote
  ): void {

    this.filtroEstado = estado;
    this.currentPage = 0;
  }

  cambiarFiltroVencimiento(
    filtro: FiltroVencimiento
  ): void {

    this.filtroVencimiento = filtro;
    this.currentPage = 0;
  }

  cambiarPagina(pagina: number): void {

    if (pagina >= 0 && pagina < this.totalPages) {
      this.currentPage = pagina;
    }
  }

  private ajustarPagina(): void {

    if (this.totalPages === 0) {

      this.currentPage = 0;

    } else if (this.currentPage >= this.totalPages) {

      this.currentPage = this.totalPages - 1;
    }
  }

  // ============================================================
  // FECHAS Y VENCIMIENTOS
  // ============================================================

  private fechaHoyISO(): string {

    return this.formatoFechaLocal(new Date());
  }

  private fechaEnDiasISO(dias: number): string {

    const fecha = new Date();

    fecha.setDate(fecha.getDate() + dias);

    return this.formatoFechaLocal(fecha);
  }

  private formatoFechaLocal(fecha: Date): string {

    const anio = fecha.getFullYear();

    const mes = String(
      fecha.getMonth() + 1
    ).padStart(2, '0');

    const dia = String(
      fecha.getDate()
    ).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  /**
   * Formatea una fecha ISO de tipo YYYY-MM-DD
   * sin convertirla a UTC ni modificar el día.
   */
  formatearFechaVencimiento(
    fecha: string | null | undefined
  ): string {

    if (!fecha) {
      return '—';
    }

    const fechaSinHora = String(fecha).split('T')[0];

    const partes = fechaSinHora.split('-');

    if (
      partes.length !== 3 ||
      !partes[0] ||
      !partes[1] ||
      !partes[2]
    ) {
      return String(fecha);
    }

    const [anio, mes, dia] = partes;

    return `${dia}/${mes}/${anio}`;
  }

  /**
   * Formatea la fecha y hora de ingreso sin utilizar
   * DatePipe, para evitar conversiones de zona horaria.
   */
  formatearFechaIngreso(
    fecha: string | null | undefined
  ): string {

    if (!fecha) {
      return '—';
    }

    const partes = String(fecha).split('T');

    const fechaParte = partes[0];

    const fechaFormateada =
      this.formatearFechaVencimiento(fechaParte);

    if (partes.length < 2 || !partes[1]) {
      return fechaFormateada;
    }

    const hora = partes[1].substring(0, 5);

    return `${fechaFormateada} ${hora}`;
  }

  esVencido(lote: Lote): boolean {

    return lote.estado === 1 &&
      Number(lote.cantidad) > 0 &&
      lote.fechaVencimiento < this.fechaHoyISO();
  }

  estaPorVencer(lote: Lote): boolean {

    const hoy = this.fechaHoyISO();
    const limite = this.fechaEnDiasISO(30);

    return lote.estado === 1 &&
      Number(lote.cantidad) > 0 &&
      lote.fechaVencimiento >= hoy &&
      lote.fechaVencimiento <= limite;
  }

  // ============================================================
  // AJUSTAR EXISTENCIAS
  // ============================================================

  abrirModalAjuste(lote: Lote): void {

    if (this.isSaving) {
      return;
    }

    const producto = this.productos.find(
      item => item.id === lote.idProducto
    );

    if (!producto) {

      this.errorCarga =
        'No se encontraron los datos del producto asociado al lote.';

      return;
    }

    this.loteSeleccionado = lote;
    this.productoAjusteSeleccionado = producto;

    const primeraPresentacion =
      (producto.presentaciones || []).find(
        presentacion => presentacion.estado === 1
      );

    this.ajusteForm.reset({
      idPresentacion: primeraPresentacion?.id ?? null,
      cantidadAjuste: null,
      motivo: 'ERROR_CONTEO_FISICO',
      observacion: ''
    });

    this.errorModal = '';
    this.mostrarModalAjuste = true;
  }

  cerrarModalAjuste(): void {

    if (this.isSaving) {
      return;
    }

    this.mostrarModalAjuste = false;
    this.loteSeleccionado = null;
    this.productoAjusteSeleccionado = null;
    this.errorModal = '';
  }

  guardarAjuste(): void {

    if (this.isSaving || !this.loteSeleccionado) {
      return;
    }

    if (this.ajusteForm.invalid) {

      this.ajusteForm.markAllAsTouched();

      this.errorModal =
        'Complete correctamente los campos del ajuste.';

      return;
    }

    const valores = this.ajusteForm.getRawValue();

    const cantidadIngresada = Number(
      valores.cantidadAjuste
    );

    if (
      !Number.isFinite(cantidadIngresada) ||
      !Number.isInteger(cantidadIngresada) ||
      cantidadIngresada === 0
    ) {

      this.errorModal =
        'Ingrese una cantidad entera distinta de cero.';

      return;
    }

    const motivo = valores.motivo as MotivoAjuste;

    const opcionMotivo = this.motivosAjuste.find(
      item => item.value === motivo
    );

    if (!opcionMotivo) {

      this.errorModal = 'Seleccione un motivo válido.';

      return;
    }

    /*
     * Para las mermas, la interfaz acepta una cantidad positiva
     * y la convierte a negativa antes de enviarla al backend.
     *
     * Para los ajustes:
     *   positivo = aumenta existencias
     *   negativo = disminuye existencias
     */
    const cantidadAjuste =
      opcionMotivo.tipoMovimiento === 'MERMA'
        ? -Math.abs(cantidadIngresada)
        : cantidadIngresada;

    const request: LoteAjusteRequest = {
      idPresentacion: Number(valores.idPresentacion),
      cantidadAjuste,
      motivo,
      observacion:
        String(valores.observacion || '').trim() || null
    };

    this.isSaving = true;
    this.errorModal = '';

    this.loteService
      .ajustarLote(
        this.loteSeleccionado.id,
        request
      )
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {

          this.mostrarModalAjuste = false;
          this.loteSeleccionado = null;
          this.productoAjusteSeleccionado = null;

          this.cargarLotes();
          this.cargarProductos();
        },

        error: (err) => {

          this.errorModal = this.obtenerMensajeError(
            err,
            'No se pudo realizar el ajuste de existencias.'
          );
        }

      });
  }

  // ============================================================
  // CONFIRMAR ACTIVACIÓN O DESACTIVACIÓN
  // ============================================================

  solicitarCambioEstado(lote: Lote): void {

    const activar = lote.estado === 0;

    this.confirmacion = {
      id: lote.id,

      tipo: activar
        ? 'activar'
        : 'desactivar',

      numeroLote: lote.numeroLote,
      nombreProducto: lote.nombreProducto,

      titulo: activar
        ? 'Activar lote'
        : 'Desactivar lote',

      mensaje: activar
        ? '¿Desea activar este lote?'
        : '¿Desea desactivar este lote? El registro se conservará, pero dejará de estar activo.',

      textoBoton: activar
        ? 'Activar'
        : 'Desactivar'
    };

    this.errorConfirmacion = '';
  }

  // ============================================================
  // CONFIRMAR ELIMINACIÓN
  // ============================================================

  solicitarEliminacion(lote: Lote): void {

    this.confirmacion = {
      id: lote.id,
      tipo: 'eliminar',
      numeroLote: lote.numeroLote,
      nombreProducto: lote.nombreProducto,

      titulo: 'Eliminar lote',

      mensaje:
        'El lote se marcará como eliminado y su existencia se retirará del stock disponible. ¿Desea continuar?',

      textoBoton: 'Eliminar'
    };

    this.errorConfirmacion = '';
  }

  cerrarConfirmacion(): void {

    if (this.procesandoConfirmacion) {
      return;
    }

    this.confirmacion = null;
    this.errorConfirmacion = '';
  }

  confirmarAccion(): void {

    const accion = this.confirmacion;

    if (!accion || this.procesandoConfirmacion) {
      return;
    }

    this.procesandoConfirmacion = true;
    this.errorConfirmacion = '';

    const peticion: Observable<unknown> =
      accion.tipo === 'eliminar'

        ? this.loteService.eliminarLote(accion.id)

        : this.loteService.cambiarEstado(
            accion.id,
            accion.tipo === 'activar' ? 1 : 0
          );

    peticion
      .pipe(
        finalize(() => {
          this.procesandoConfirmacion = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {

          this.confirmacion = null;

          this.cargarLotes();
          this.cargarProductos();
        },

        error: (err) => {

          this.errorConfirmacion = this.obtenerMensajeError(
            err,
            'No se pudo completar la operación.'
          );
        }

      });
  }

  // ============================================================
  // ETIQUETAS DE PRESENTACIÓN
  // ============================================================

  etiquetaTipo(tipo: TipoPresentacion): string {

    const etiquetas: Record<TipoPresentacion, string> = {
      UNIDAD: 'Unidad',
      BLISTER: 'Blíster',
      CAJA: 'Caja',
      FRASCO: 'Frasco',
      TIRA: 'Tira',
      OTRO: 'Otro'
    };

    return etiquetas[tipo] || tipo;
  }

  // ============================================================
  // MANEJO DE ERRORES
  // ============================================================

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

      return mensaje ||
        'No tiene permisos para realizar esta operación.';
    }

    if (err?.status === 0) {

      return 'No se pudo conectar con el servidor.';
    }

    return mensaje || mensajePredeterminado;
  }
}


