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
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { Lote, LoteAjusteRequest, LoteRequest, MotivoAjuste } from '../../../core/models/lote.model';
import { Producto, Presentacion } from '../../../core/models/producto.model';

import { LoteService } from '../services/lote';
import { ProductoService } from '../../productos/services/producto';

export type TipoModalLote = 'ingreso' | 'ajuste';

interface OpcionMotivoAjuste {
  valor: MotivoAjuste;
  etiqueta: string;
  tipo: 'merma' | 'ajuste';
}

@Component({
  selector: 'app-lote-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './lote-modal.html',
  styleUrls: ['./lote-modal.scss']
})
export class LoteModalComponent implements OnInit {

  @Input() modo: TipoModalLote = 'ingreso';
  @Input() loteAjuste: Lote | null = null;

  @Output() cerrar = new EventEmitter<void>();
  @Output() guardado = new EventEmitter<void>();

  private loteService = inject(LoteService);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  productos: Producto[] = [];

  cargandoProductos = false;
  procesando = false;

  errorCargaProductos = '';
  errorIngresoModal = '';
  errorAjusteModal = '';

  // ============================================================
  // DATOS DEL FORMULARIO DE INGRESO
  // ============================================================

  busquedaProductoIngreso = '';
  mostrarOpcionesProductoIngreso = false;
  productoIngresoSeleccionado: Producto | null = null;

  numeroLote = '';
  fechaVencimiento = '';

  cantidadesIngreso: Record<number, number> = {};

  // ============================================================
  // DATOS DEL FORMULARIO DE AJUSTE
  // ============================================================

  idPresentacionAjuste: number | null = null;
  cantidadAjuste: number | null = null;
  motivoAjuste: MotivoAjuste | null = null;
  observacionAjuste = '';

  readonly motivosAjuste: OpcionMotivoAjuste[] = [
    {
      valor: 'PRODUCTO_VENCIDO',
      etiqueta: 'Producto vencido',
      tipo: 'merma'
    },
    {
      valor: 'PRODUCTO_DANADO',
      etiqueta: 'Producto dañado o roto',
      tipo: 'merma'
    },
    {
      valor: 'PERDIDA_ROBO',
      etiqueta: 'Pérdida por robo',
      tipo: 'merma'
    },
    {
      valor: 'DEVOLUCION_PROVEEDOR',
      etiqueta: 'Devolución a proveedor',
      tipo: 'merma'
    },
    {
      valor: 'USO_INTERNO_DONACION',
      etiqueta: 'Uso interno o donación',
      tipo: 'merma'
    },
    {
      valor: 'ERROR_CONTEO_FISICO',
      etiqueta: 'Faltante o sobrante en conteo',
      tipo: 'ajuste'
    },
    {
      valor: 'ERROR_DIGITACION',
      etiqueta: 'Error de digitación',
      tipo: 'ajuste'
    },
    {
      valor: 'AJUSTE_EMPAQUE',
      etiqueta: 'Ajuste por apertura de empaque',
      tipo: 'ajuste'
    }
  ];

  ngOnInit(): void {
    this.cargarProductos();
  }

  // ============================================================
  // FECHAS
  // ============================================================

  get fechaHoy(): string {
    return this.convertirFechaAString(new Date());
  }

  get fechaMinimaVencimiento(): string {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 1);

    return this.convertirFechaAString(fecha);
  }

  private convertirFechaAString(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }

  // ============================================================
  // CARGAR PRODUCTOS
  // ============================================================

  cargarProductos(): void {
    this.cargandoProductos = true;
    this.errorCargaProductos = '';

    this.productoService.listarProductos()
      .pipe(
        finalize(() => {
          this.cargandoProductos = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: respuesta => {
          this.productos = respuesta || [];
        },
        error: (err: any) => {
          console.error('Error al cargar productos para el modal:', err);

          this.productos = [];
          this.errorCargaProductos = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los productos. Inténtelo nuevamente.'
          );
        }
      });
  }

  // ============================================================
  // BÚSQUEDA DE PRODUCTOS PARA EL INGRESO
  // ============================================================

  get productosCoincidentesIngreso(): Producto[] {
    const termino = this.busquedaProductoIngreso
      .trim()
      .toLocaleLowerCase();

    return this.productos
      .filter(producto => {
        const tienePresentacionActiva =
          (producto.presentaciones || []).some(
            presentacion => presentacion.estado === 1
          );

        const productoDisponible =
          producto.estado === 1 && tienePresentacionActiva;

        const coincideTexto =
          !termino ||
          (producto.nombre || '').toLocaleLowerCase().includes(termino) ||
          (producto.categoriaNombre || '').toLocaleLowerCase().includes(termino);

        return productoDisponible && coincideTexto;
      })
      .slice(0, termino ? 8 : 5);
  }

  buscarProductoIngreso(valor: string): void {
    this.busquedaProductoIngreso = valor;

    if (
      this.productoIngresoSeleccionado &&
      valor.trim().toLocaleLowerCase() !==
        this.productoIngresoSeleccionado.nombre.trim().toLocaleLowerCase()
    ) {
      this.productoIngresoSeleccionado = null;
      this.cantidadesIngreso = {};
    }

    this.mostrarOpcionesProductoIngreso = true;
  }

  seleccionarProductoIngreso(producto: Producto): void {
    this.productoIngresoSeleccionado = producto;
    this.busquedaProductoIngreso = producto.nombre;
    this.mostrarOpcionesProductoIngreso = false;
    this.cantidadesIngreso = {};

    for (const presentacion of this.presentacionesIngreso) {
      this.cantidadesIngreso[presentacion.id] = 0;
    }
  }

  get presentacionesIngreso(): Presentacion[] {
    if (!this.productoIngresoSeleccionado) {
      return [];
    }

    return (this.productoIngresoSeleccionado.presentaciones || [])
      .filter(presentacion => presentacion.estado === 1);
  }

  etiquetaPresentacion(presentacion: Presentacion): string {
    const nombres: Record<string, string> = {
      UNIDAD: 'Unidad',
      BLISTER: 'Blíster',
      CAJA: 'Caja',
      FRASCO: 'Frasco',
      TIRA: 'Tira',
      OTRO: 'Otro'
    };

    const nombre = nombres[presentacion.tipo] || presentacion.tipo;

    return `${nombre} · ${presentacion.factorConversion} unidad(es) base`;
  }

  // ============================================================
  // CERRAR MODAL DE INGRESO
  // ============================================================

  cerrarModalIngreso(): void {
    if (this.procesando) {
      return;
    }

    this.cerrar.emit();
  }

  // ============================================================
  // REGISTRAR INGRESO
  // ============================================================

  registrarIngreso(): void {
    this.errorIngresoModal = '';

    if (this.modo !== 'ingreso' || this.procesando) {
      return;
    }

    const producto = this.productoIngresoSeleccionado;

    if (!producto) {
      this.errorIngresoModal =
        'Busca y selecciona un producto antes de registrar el ingreso.';
      return;
    }

    if (!this.numeroLote.trim()) {
      this.errorIngresoModal =
        'Ingresa el número de lote que aparece en el envase.';
      return;
    }

    if (!this.fechaVencimiento) {
      this.errorIngresoModal =
        'Selecciona la fecha de vencimiento.';
      return;
    }

    if (this.fechaVencimiento <= this.fechaHoy) {
      this.errorIngresoModal =
        'La fecha de vencimiento debe ser posterior a hoy.';
      return;
    }

    const cantidades = this.presentacionesIngreso.map(presentacion => ({
      idPresentacion: presentacion.id,
      cantidadPresentacion: Number(
        this.cantidadesIngreso[presentacion.id] ?? 0
      )
    }));

    const cantidadInvalida = cantidades.some(detalle =>
      !Number.isInteger(detalle.cantidadPresentacion) ||
      detalle.cantidadPresentacion < 0
    );

    if (cantidadInvalida) {
      this.errorIngresoModal =
        'Las cantidades deben ser números enteros iguales o mayores que cero.';
      return;
    }

    const detallesIngreso = cantidades.filter(
      detalle => detalle.cantidadPresentacion > 0
    );

    if (detallesIngreso.length === 0) {
      this.errorIngresoModal =
        'Ingresa una cantidad mayor que cero en al menos una presentación.';
      return;
    }

    const request: LoteRequest = {
      numeroLote: this.numeroLote.trim(),
      fechaVencimiento: this.fechaVencimiento,
      detallesIngreso
    };

    this.procesando = true;

    this.loteService.crearLote(producto.id, request)
      .pipe(
        finalize(() => {
          this.procesando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.guardado.emit();
          this.cerrar.emit();
        },
        error: (err: any) => {
          this.errorIngresoModal = this.obtenerMensajeError(
            err,
            'No se pudo registrar el ingreso del lote.'
          );
        }
      });
  }

  // ============================================================
  // PRESENTACIONES DEL LOTE PARA EL AJUSTE
  // ============================================================

  get presentacionesDelLote(): Presentacion[] {
    if (!this.loteAjuste) {
      return [];
    }

    const producto = this.productos.find(
      item => item.id === this.loteAjuste?.idProducto
    );

    return (producto?.presentaciones || [])
      .filter(presentacion => presentacion.estado === 1);
  }

  get motivoSeleccionado(): OpcionMotivoAjuste | undefined {
    if (!this.motivoAjuste) {
      return undefined;
    }

    return this.motivosAjuste.find(
      motivo => motivo.valor === this.motivoAjuste
    );
  }

  get esMotivoMerma(): boolean {
    return this.motivoSeleccionado?.tipo === 'merma';
  }

  // ============================================================
  // CERRAR MODAL DE AJUSTE
  // ============================================================

  cerrarModalAjuste(): void {
    if (this.procesando) {
      return;
    }

    this.cerrar.emit();
  }

  // ============================================================
  // GUARDAR AJUSTE DE EXISTENCIAS
  // ============================================================

  guardarAjuste(): void {
    this.errorAjusteModal = '';

    if (
      this.modo !== 'ajuste' ||
      !this.loteAjuste ||
      this.procesando
    ) {
      return;
    }

    if (this.idPresentacionAjuste === null) {
      this.errorAjusteModal =
        'Selecciona la presentación para realizar el ajuste.';
      return;
    }

    if (this.motivoAjuste === null) {
      this.errorAjusteModal =
        'Selecciona un motivo para realizar el ajuste.';
      return;
    }

    if (
      this.cantidadAjuste === null ||
      !Number.isFinite(Number(this.cantidadAjuste)) ||
      !Number.isInteger(Number(this.cantidadAjuste)) ||
      Number(this.cantidadAjuste) === 0
    ) {
      this.errorAjusteModal =
        'Ingresa una cantidad entera distinta de cero.';
      return;
    }

    const motivoSeleccionado = this.motivoAjuste;
    let cantidadEnviar = Number(this.cantidadAjuste);

    if (this.esMotivoMerma) {
      if (cantidadEnviar <= 0) {
        this.errorAjusteModal =
          'Para una merma, ingresa la cantidad a descontar como positiva.';
        return;
      }

      // El backend recibe las mermas como cantidades negativas.
      cantidadEnviar = -Math.abs(cantidadEnviar);
    }

    const request: LoteAjusteRequest = {
      idPresentacion: this.idPresentacionAjuste,
      cantidadAjuste: cantidadEnviar,
      motivo: motivoSeleccionado,
      observacion: this.observacionAjuste.trim() || null
    };

    this.procesando = true;

    this.loteService.ajustarLote(this.loteAjuste.id, request)
      .pipe(
        finalize(() => {
          this.procesando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.guardado.emit();
          this.cerrar.emit();
        },
        error: (err: any) => {
          this.errorAjusteModal = this.obtenerMensajeError(
            err,
            'No se pudo ajustar el stock del lote.'
          );
        }
      });
  }

  // ============================================================
  // MANEJO DE ERRORES
  // ============================================================

  private obtenerMensajeError(
    err: any,
    mensajePredeterminado: string
  ): string {
    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }

    if (err?.status === 403) {
      return err?.error?.mensaje ||
        'No tiene permisos para realizar esta operación.';
    }

    return err?.error?.mensaje ||
      err?.error?.message ||
      mensajePredeterminado;
  }

  // ============================================================
  // CERRAR SUGERENCIAS AL HACER CLIC DENTRO DEL MODAL
  // ============================================================

  manejarClickDentroDelModal(event: MouseEvent): void {
    event.stopPropagation();

    const objetivo = event.target;

    if (!(objetivo instanceof Element)) {
      return;
    }

    const dentroDelBuscador =
      objetivo.closest('.product-picker-input');

    const dentroDeLasSugerencias =
      objetivo.closest('.product-suggestions');

    const botonCambiarProducto =
      objetivo.closest('.selected-product .text-button');

    if (
      dentroDelBuscador ||
      dentroDeLasSugerencias ||
      botonCambiarProducto
    ) {
      return;
    }

    this.mostrarOpcionesProductoIngreso = false;
  }
}


