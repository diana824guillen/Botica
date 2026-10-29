import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, finalize } from 'rxjs';

import { LoteService } from '../services/lote';

import {
  Lote,
  LoteAjusteRequest,
  LoteRequest,
  MotivoAjuste
} from '../../../core/models/lote.model';

import {
  Producto,
  Presentacion
} from '../../../core/models/producto.model';

import { ProductoService } from '../../productos/services/producto';
import { HasPermissionDirective } from '../../../core/directives/has-permission';

interface OpcionMotivoAjuste {
  valor: MotivoAjuste;
  etiqueta: string;
  tipo: 'merma' | 'ajuste';
}

type FiltroEstado = 'todos' | 'activo' | 'inactivo';
type FiltroVencimiento = 'todos' | 'vencidos' | 'rango';

type TipoConfirmacionLote =
  | 'activar'
  | 'desactivar'
  | 'eliminar';

interface ConfirmacionLote {
  id: number;
  tipo: TipoConfirmacionLote;
  numeroLote: string;
  nombreProducto: string;
  titulo: string;
  mensaje: string;
  textoBoton: string;
}

@Component({
  selector: 'app-lotes-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HasPermissionDirective
  ],
  templateUrl: './lotes-list.html',
  styleUrls: ['./lotes-list.scss']
})
export class LotesListComponent implements OnInit {

  private loteService = inject(LoteService);
  private productoService = inject(ProductoService);
  private cdr = inject(ChangeDetectorRef);

  // Datos principales.
  lotes: Lote[] = [];
  productos: Producto[] = [];

  cargandoLotes = false;
  cargandoProductos = false;
  procesando = false;

  errorCarga = '';
  mensaje = '';
  tipoMensaje: 'exito' | 'error' = 'exito';

  // Búsqueda y filtros de la tabla.
  busqueda = '';
  filtroEstado: FiltroEstado = 'todos';
  filtroVencimiento: FiltroVencimiento = 'todos';

  fechaDesde = '';
  fechaHasta = '';

  // Registro de ingreso.
  mostrarModalIngreso = false;

  busquedaProductoIngreso = '';
  mostrarOpcionesProductoIngreso = false;
  productoIngresoSeleccionado: Producto | null = null;

  numeroLote = '';
  fechaVencimiento = '';

  cantidadesIngreso: Record<number, number> = {};

  // Ajuste de existencias.
  mostrarModalAjuste = false;
  loteAjuste: Lote | null = null;

  idPresentacionAjuste: number | null = null;
  cantidadAjuste: number | null = null;

  motivoAjuste: MotivoAjuste = 'ERROR_CONTEO_FISICO';
  observacionAjuste = '';

  // Confirmación de acciones.
  confirmacionLote: ConfirmacionLote | null = null;
  errorConfirmacionLote = '';

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
    this.cargarLotes();
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

  get rangoFechasValido(): boolean {
    return !!this.fechaDesde
      && !!this.fechaHasta
      && this.fechaDesde <= this.fechaHasta;
  }

  // ============================================================
  // LISTADO Y ORDEN VISUAL
  // ============================================================

  get lotesOrdenados(): Lote[] {
    return [...this.lotes].sort((a, b) => {
      const fechaA = a.fechaIngreso || '';
      const fechaB = b.fechaIngreso || '';

      // Primero la fecha de ingreso más reciente.
      const comparacionFecha = fechaB.localeCompare(fechaA);

      if (comparacionFecha !== 0) {
        return comparacionFecha;
      }

      // Si el ingreso tiene la misma fecha, usar el ID descendente.
      return b.id - a.id;
    });
  }

  get lotesFiltrados(): Lote[] {
    const termino = this.busqueda.trim().toLocaleLowerCase();

    return this.lotesOrdenados.filter(lote => {

      const coincideBusqueda =
        !termino ||
        (lote.numeroLote || '').toLocaleLowerCase().includes(termino) ||
        (lote.nombreProducto || '').toLocaleLowerCase().includes(termino);

      const coincideEstado =
        this.filtroEstado === 'todos' ||
        (this.filtroEstado === 'activo' && lote.estado === 1) ||
        (this.filtroEstado === 'inactivo' && lote.estado === 0);

      const fecha = (lote.fechaVencimiento || '').slice(0, 10);

      let coincideVencimiento = true;

      if (this.filtroVencimiento === 'vencidos') {
        coincideVencimiento =
          lote.estado === 1 &&
          Number(lote.cantidad) > 0 &&
          fecha < this.fechaHoy;
      }

      if (this.filtroVencimiento === 'rango') {
        coincideVencimiento =
          this.rangoFechasValido &&
          lote.estado === 1 &&
          Number(lote.cantidad) > 0 &&
          fecha >= this.fechaDesde &&
          fecha <= this.fechaHasta;
      }

      return coincideBusqueda && coincideEstado && coincideVencimiento;
    });
  }

  // ============================================================
  // CARGA DE DATOS
  // ============================================================

  cargarLotes(): void {
    this.cargandoLotes = true;
    this.errorCarga = '';

    this.loteService.listarLotes()
      .pipe(
        finalize(() => {
          this.cargandoLotes = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: respuesta => {
          this.lotes = respuesta || [];
        },
        error: err => {
          console.error('Error al cargar los lotes:', err);

          this.errorCarga = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los lotes.'
          );
        }
      });
  }

  cargarProductos(): void {
    this.cargandoProductos = true;

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
        error: err => {
          console.error('Error al cargar los productos:', err);
        }
      });
  }

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

  private mostrarMensaje(
    texto: string,
    tipo: 'exito' | 'error'
  ): void {
    this.mensaje = texto;
    this.tipoMensaje = tipo;
  }

  // ============================================================
  // FILTRO DE PRODUCTOS PARA EL INGRESO
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
          (producto.nombre || '')
            .toLocaleLowerCase()
            .includes(termino) ||
          (producto.categoriaNombre || '')
            .toLocaleLowerCase()
            .includes(termino);

        return productoDisponible && coincideTexto;
      })
      .slice(0, termino ? 8 : 5);
  }

  buscarProductoIngreso(valor: string): void {
    this.busquedaProductoIngreso = valor;

    if (
      this.productoIngresoSeleccionado &&
      valor.trim().toLocaleLowerCase() !==
        this.productoIngresoSeleccionado.nombre
          .trim()
          .toLocaleLowerCase()
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
  // MODAL DE NUEVO INGRESO
  // ============================================================

  abrirModalIngreso(): void {
    this.limpiarFormularioIngreso();
    this.mostrarModalIngreso = true;

    this.mostrarOpcionesProductoIngreso = false;
  }

  cerrarModalIngreso(): void {
    if (this.procesando) {
      return;
    }

    this.mostrarModalIngreso = false;
    this.limpiarFormularioIngreso();
  }

  private limpiarFormularioIngreso(): void {
    this.busquedaProductoIngreso = '';
    this.mostrarOpcionesProductoIngreso = false;
    this.productoIngresoSeleccionado = null;

    this.numeroLote = '';
    this.fechaVencimiento = '';

    this.cantidadesIngreso = {};
  }

  registrarIngreso(): void {
    if (this.procesando) {
      return;
    }

    const producto = this.productoIngresoSeleccionado;

    if (!producto) {
      this.mostrarMensaje(
        'Busca y selecciona un producto antes de registrar el ingreso.',
        'error'
      );
      return;
    }

    if (!this.numeroLote.trim()) {
      this.mostrarMensaje(
        'Ingresa el número de lote que aparece en el envase.',
        'error'
      );
      return;
    }

    if (!this.fechaVencimiento) {
      this.mostrarMensaje(
        'Selecciona la fecha de vencimiento.',
        'error'
      );
      return;
    }

    if (this.fechaVencimiento <= this.fechaHoy) {
      this.mostrarMensaje(
        'La fecha de vencimiento debe ser posterior a hoy.',
        'error'
      );
      return;
    }

    const detallesIngreso = this.presentacionesIngreso
      .map(presentacion => ({
        idPresentacion: presentacion.id,
        cantidadPresentacion:
          Number(this.cantidadesIngreso[presentacion.id] || 0)
      }))
      .filter(detalle => detalle.cantidadPresentacion > 0);

    if (detallesIngreso.length === 0) {
      this.mostrarMensaje(
        'Ingresa una cantidad mayor que cero en al menos una presentación.',
        'error'
      );
      return;
    }

    const cantidadesValidas = detallesIngreso.every(
      detalle =>
        Number.isInteger(detalle.cantidadPresentacion) &&
        detalle.cantidadPresentacion > 0
    );

    if (!cantidadesValidas) {
      this.mostrarMensaje(
        'Las cantidades deben ser números enteros positivos.',
        'error'
      );
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
          this.mostrarModalIngreso = false;
          this.limpiarFormularioIngreso();

          this.mostrarMensaje(
            'El ingreso del lote se registró correctamente.',
            'exito'
          );

          this.cargarLotes();
          this.cargarProductos();
        },
        error: err => {
          this.mostrarMensaje(
            this.obtenerMensajeError(
              err,
              'No se pudo registrar el ingreso del lote.'
            ),
            'error'
          );
        }
      });
  }

  // ============================================================
  // AJUSTE DE EXISTENCIAS
  // ============================================================

  abrirModalAjuste(lote: Lote): void {
    this.loteAjuste = lote;
    this.idPresentacionAjuste = null;
    this.cantidadAjuste = null;

    this.motivoAjuste = 'ERROR_CONTEO_FISICO';
    this.observacionAjuste = '';

    const presentaciones = this.presentacionesDelLote;

    if (presentaciones.length > 0) {
      this.idPresentacionAjuste = presentaciones[0].id;
    }

    this.mostrarModalAjuste = true;
  }

  cerrarModalAjuste(): void {
    if (this.procesando) {
      return;
    }

    this.mostrarModalAjuste = false;
    this.loteAjuste = null;
  }

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
    return this.motivosAjuste.find(
      motivo => motivo.valor === this.motivoAjuste
    );
  }

  get esMotivoMerma(): boolean {
    return this.motivoSeleccionado?.tipo === 'merma';
  }

  guardarAjuste(): void {
    if (!this.loteAjuste || this.procesando) {
      return;
    }

    if (!this.idPresentacionAjuste) {
      this.mostrarMensaje(
        'Selecciona la presentación para realizar el ajuste.',
        'error'
      );
      return;
    }

    if (
      this.cantidadAjuste === null ||
      !Number.isFinite(Number(this.cantidadAjuste)) ||
      !Number.isInteger(Number(this.cantidadAjuste)) ||
      Number(this.cantidadAjuste) === 0
    ) {
      this.mostrarMensaje(
        'Ingresa una cantidad entera distinta de cero.',
        'error'
      );
      return;
    }

    let cantidadEnviar = Number(this.cantidadAjuste);

    // La interfaz solicita una cantidad positiva para una merma.
    // El backend recibe la cantidad negativa que debe descontar.
    if (this.esMotivoMerma) {
      if (cantidadEnviar <= 0) {
        this.mostrarMensaje(
          'Para una merma, ingresa la cantidad a descontar como positiva.',
          'error'
        );
        return;
      }

      cantidadEnviar = -Math.abs(cantidadEnviar);
    }

    const request: LoteAjusteRequest = {
      idPresentacion: this.idPresentacionAjuste,
      cantidadAjuste: cantidadEnviar,
      motivo: this.motivoAjuste,
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
          this.mostrarModalAjuste = false;
          this.loteAjuste = null;

          this.mostrarMensaje(
            'Las existencias del lote se actualizaron correctamente.',
            'exito'
          );

          this.cargarLotes();
          this.cargarProductos();
        },
        error: err => {
          this.mostrarMensaje(
            this.obtenerMensajeError(
              err,
              'No se pudo ajustar el stock del lote.'
            ),
            'error'
          );
        }
      });
  }

  // ============================================================
  // CONFIRMAR ACTIVAR, DESACTIVAR Y ELIMINAR
  // ============================================================

  solicitarCambioEstado(lote: Lote): void {
    const activar = lote.estado !== 1;

    this.confirmacionLote = {
      id: lote.id,
      tipo: activar ? 'activar' : 'desactivar',
      numeroLote: lote.numeroLote,
      nombreProducto: lote.nombreProducto,

      titulo: activar
        ? 'Activar lote'
        : 'Desactivar lote',

      mensaje: activar
        ? '¿Deseas activar este lote para que vuelva a estar disponible en el inventario?'
        : '¿Deseas desactivar este lote? Se conservará su registro, pero dejará de estar disponible como lote activo.',

      textoBoton: activar ? 'Activar lote' : 'Desactivar lote'
    };

    this.errorConfirmacionLote = '';
  }

  solicitarEliminacion(lote: Lote): void {
    this.confirmacionLote = {
      id: lote.id,
      tipo: 'eliminar',
      numeroLote: lote.numeroLote,
      nombreProducto: lote.nombreProducto,

      titulo: 'Eliminar lote',

      mensaje:
        '¿Estás seguro de eliminar este lote? Esta acción lo retirará de los lotes disponibles y se conservará el registro de la operación.',

      textoBoton: 'Eliminar lote'
    };

    this.errorConfirmacionLote = '';
  }

  cerrarConfirmacionLote(): void {
    if (this.procesando) {
      return;
    }

    this.confirmacionLote = null;
    this.errorConfirmacionLote = '';
  }

  confirmarAccionLote(): void {
    const accion = this.confirmacionLote;

    if (!accion || this.procesando) {
      return;
    }

    this.procesando = true;
    this.errorConfirmacionLote = '';

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
          this.procesando = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          // Cerrar la confirmación sin mostrar mensajes de éxito.
          this.confirmacionLote = null;
          this.errorConfirmacionLote = '';

          // Actualizar la tabla y los datos de productos.
          this.cargarLotes();
          this.cargarProductos();
        },

        error: (err: any) => {
          this.errorConfirmacionLote = this.obtenerMensajeError(
            err,
            'No se pudo completar la operación. Inténtalo nuevamente.'
          );
        }
      });
  }

  // ============================================================
  // INDICADORES Y UTILIDADES DE LA TABLA
  // ============================================================

  esVencido(lote: Lote): boolean {
    return lote.estado === 1 &&
      Number(lote.cantidad) > 0 &&
      lote.fechaVencimiento < this.fechaHoy;
  }

  esPorVencer(lote: Lote): boolean {
    if (
      lote.estado !== 1 ||
      Number(lote.cantidad) <= 0 ||
      this.esVencido(lote)
    ) {
      return false;
    }

    const limite = new Date();
    limite.setDate(limite.getDate() + 30);

    const fechaLimite = this.convertirFechaAString(limite);

    return lote.fechaVencimiento <= fechaLimite;
  }

  limpiarMensaje(): void {
    this.mensaje = '';
  }

  // ============================================================
  // CERRAR SUGERENCIAS AL HACER CLICK DENTRO DEL MODAL
  // ============================================================
  manejarClickDentroDelModal(event: MouseEvent): void {
    // Evitar que el clic cierre el modal desde el fondo.
    event.stopPropagation();

    const objetivo = event.target;

    if (!(objetivo instanceof Element)) {
      return;
    }

    // Mantener las sugerencias abiertas al interactuar
    // con el buscador o seleccionar una sugerencia.
    const dentroDelBuscador =
      objetivo.closest('.product-picker-input');

    const dentroDeLasSugerencias =
      objetivo.closest('.product-suggestions');

    // Al pulsar "Cambiar", se debe permitir abrir de nuevo la búsqueda.
    const botonCambiarProducto =
      objetivo.closest('.selected-product .text-button');

    if (
      dentroDelBuscador ||
      dentroDeLasSugerencias ||
      botonCambiarProducto
    ) {
      return;
    }

    // Cualquier otro clic dentro del modal cierra las sugerencias.
    this.mostrarOpcionesProductoIngreso = false;
  }


}


