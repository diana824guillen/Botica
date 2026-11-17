import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';

import {
  Subscription,
  finalize
} from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

import {
  DashboardInventarioResponse,
  DashboardPresentacionAlerta
} from '../../../core/models/dashboard.model';

import {
  TipoPresentacion
} from '../../../core/models/producto.model';

import {
  MovimientoInventario
} from '../../../core/models/movimiento-inventario.model';

import {
  DashboardService
} from '../services/dashboard';


type PeriodoDashboard =
  | 'HOY'
  | 'SEMANA'
  | 'MES'
  | 'ANIO'
  | 'PERSONALIZADO';

type DiasAnticipacionDashboard = 7 | 15 | 30;

type EstadoAlertaStock = 'AGOTADO' | 'REPONER';


interface RangoFechasDashboard {
  desde: Date;
  hasta: Date;
  desdeComparacion: Date;
  hastaComparacion: Date;
}


/**
 * Estructuras temporales para mantener funcionando
 * la plantilla actual mientras se desarrolla la nueva.
 */
interface PresentacionAlertaDashboardVista {
  id: number;
  tipo: TipoPresentacion;
  factorConversion: number;
  stockDisponible: number;
  stockMinimo: number;
  estadoAlerta: EstadoAlertaStock;
}

interface ProductoAlertaDashboardVista {
  id: number;
  nombre: string;
  presentaciones: PresentacionAlertaDashboardVista[];
}


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly cdr = inject(ChangeDetectorRef);


  // ============================================================
  // RESUMEN DE VENTAS
  // ============================================================

  resumenVentas = null as import(
    '../../../core/models/dashboard.model'
  ).DashboardVentasResponse | null;

  cargandoVentas = false;
  errorVentas = '';

  periodoSeleccionado: PeriodoDashboard = 'MES';

  fechaDesdePersonalizada = '';
  fechaHastaPersonalizada = '';

  errorFiltroFechas = '';


  // ============================================================
  // RESUMEN DE INVENTARIO
  // ============================================================

  resumenInventario: DashboardInventarioResponse | null = null;

  diasAnticipacion: DiasAnticipacionDashboard = 30;

  cargandoInventario = false;
  errorInventario = '';


  // ============================================================
  // MOVIMIENTOS RECIENTES
  // ============================================================

  movimientosRecientes: MovimientoInventario[] = [];

  cargandoMovimientos = false;
  errorMovimientos = '';


  // ============================================================
  // COMPATIBILIDAD TEMPORAL CON LA PLANTILLA ACTUAL
  // Se eliminará cuando se reorganice dashboard.html.
  // ============================================================

  productosStockBajo: ProductoAlertaDashboardVista[] = [];


  // ============================================================
  // SUSCRIPCIONES Y CONTROL DE SOLICITUDES
  // ============================================================

  private permisosSubscription: Subscription | null = null;

  private ventasSubscription: Subscription | null = null;
  private inventarioSubscription: Subscription | null = null;
  private movimientosSubscription: Subscription | null = null;

  /**
   * Identificadores para impedir que una respuesta de una
   * solicitud cancelada modifique el estado de una consulta nueva.
   */
  private ventasRequestVersion = 0;
  private inventarioRequestVersion = 0;
  private movimientosRequestVersion = 0;


  // ============================================================
  // CONTROL DE CAMBIOS DE PERMISOS
  // ============================================================

  private ultimoAccesoVentas: boolean | null = null;
  private ultimoAccesoInventario: boolean | null = null;
  private ultimoAccesoMovimientos: boolean | null = null;


  // ============================================================
  // CICLO DE VIDA
  // ============================================================

  ngOnInit(): void {

    /**
     * AuthService utiliza un BehaviorSubject.
     * La suscripción recibe inmediatamente los permisos actuales
     * y también los cambios posteriores durante la sesión.
     */
    this.permisosSubscription =
      this.authService.permissions$.subscribe(() => {
        this.sincronizarSeccionesPorPermisos();
      });
  }


  ngOnDestroy(): void {

    this.permisosSubscription?.unsubscribe();
    this.permisosSubscription = null;

    this.cancelarSolicitudVentas();
    this.cancelarSolicitudInventario();
    this.cancelarSolicitudMovimientos();
  }


  // ============================================================
  // PERMISOS
  // ============================================================

  get puedeConsultarVentas(): boolean {

    return this.authService.hasPermission(
      'HISTORIAL_VENTAS_ACCESO'
    );
  }


  get puedeConsultarInventario(): boolean {

    return this.authService.hasAnyPermission([
      'PRODUCTO_ACCESO',
      'LOTE_ACCESO'
    ]);
  }


  get puedeConsultarMovimientos(): boolean {

    return this.authService.hasPermission(
      'MOVIMIENTO_INVENTARIO_ACCESO'
    );
  }


  /**
   * Carga o limpia cada sección cuando cambia el conjunto
   * de permisos del usuario.
   */
  private sincronizarSeccionesPorPermisos(): void {

    const accesoVentas = this.puedeConsultarVentas;

    if (accesoVentas !== this.ultimoAccesoVentas) {

      this.ultimoAccesoVentas = accesoVentas;

      if (accesoVentas) {
        this.cargarResumenVentas();
      } else {
        this.limpiarResumenVentas();
      }
    }


    const accesoInventario = this.puedeConsultarInventario;

    if (accesoInventario !== this.ultimoAccesoInventario) {

      this.ultimoAccesoInventario = accesoInventario;

      if (accesoInventario) {
        this.cargarResumenInventario();
      } else {
        this.limpiarResumenInventario();
      }
    }


    const accesoMovimientos = this.puedeConsultarMovimientos;

    if (accesoMovimientos !== this.ultimoAccesoMovimientos) {

      this.ultimoAccesoMovimientos = accesoMovimientos;

      if (accesoMovimientos) {
        this.cargarMovimientosRecientes();
      } else {
        this.limpiarMovimientosRecientes();
      }
    }
  }


  // ============================================================
  // FILTROS DE VENTAS
  // ============================================================

  seleccionarPeriodo(periodo: PeriodoDashboard): void {

    this.periodoSeleccionado = periodo;
    this.errorFiltroFechas = '';

    if (periodo === 'PERSONALIZADO') {

      // Esperar a que el usuario seleccione ambas fechas.
      this.cancelarSolicitudVentas();

      this.resumenVentas = null;
      this.errorVentas = '';

      return;
    }

    this.cargarResumenVentas();
  }


  aplicarFechasPersonalizadas(): void {

    this.periodoSeleccionado = 'PERSONALIZADO';

    this.cargarResumenVentas();
  }


  /**
   * Consulta las ventas usando el periodo seleccionado.
   * Los rangos actuales y comparativos se calculan aquí,
   * no en la plantilla.
   */
  cargarResumenVentas(): void {

    if (!this.puedeConsultarVentas) {
      return;
    }

    const rango = this.obtenerRangoSeleccionado();

    if (!rango) {

      this.cancelarSolicitudVentas();

      this.resumenVentas = null;
      this.errorVentas = '';

      return;
    }

    this.consultarResumenVentas(rango);
  }


  private consultarResumenVentas(
    rango: RangoFechasDashboard
  ): void {

    this.cancelarSolicitudVentas();

    const requestVersion = this.ventasRequestVersion;

    this.resumenVentas = null;
    this.errorVentas = '';
    this.errorFiltroFechas = '';

    this.cargandoVentas = true;
    this.cdr.markForCheck();

    this.ventasSubscription = this.dashboardService
      .obtenerResumenVentas(
        this.formatearFechaLocal(rango.desde),
        this.formatearFechaLocal(rango.hasta),
        this.formatearFechaLocal(rango.desdeComparacion),
        this.formatearFechaLocal(rango.hastaComparacion)
      )
      .pipe(
        finalize(() => {
          if (requestVersion === this.ventasRequestVersion) {

            this.cargandoVentas = false;
            this.ventasSubscription = null;

            this.cdr.markForCheck();
          }
        })
      )
      .subscribe({

        next: (respuesta) => {

          if (requestVersion !== this.ventasRequestVersion) {
            return;
          }

          this.resumenVentas = respuesta;
          this.cdr.markForCheck();
        },

        error: (error: unknown) => {

          if (requestVersion !== this.ventasRequestVersion) {
            return;
          }

          console.error(
            'Error al cargar el resumen de ventas:',
            error
          );

          this.resumenVentas = null;

          this.errorVentas = this.obtenerMensajeError(
            error,
            'No se pudo cargar el resumen de ventas.'
          );

          this.cdr.markForCheck();
        }

      });
  }


  // ============================================================
  // CONSTRUCCIÓN DE PERIODOS DE VENTAS
  // ============================================================

  private obtenerRangoSeleccionado(): RangoFechasDashboard | null {

    const hoy = this.obtenerHoyLocal();

    switch (this.periodoSeleccionado) {

      case 'HOY': {

        const ayer = this.agregarDias(hoy, -1);

        return {
          desde: hoy,
          hasta: hoy,
          desdeComparacion: ayer,
          hastaComparacion: ayer
        };
      }


      case 'SEMANA': {

        // La semana comienza el lunes.
        const desplazamiento = (hoy.getDay() + 6) % 7;

        const lunesActual = this.agregarDias(
          hoy,
          -desplazamiento
        );

        return {
          desde: lunesActual,
          hasta: hoy,
          desdeComparacion: this.agregarDias(
            lunesActual,
            -7
          ),
          hastaComparacion: this.agregarDias(
            hoy,
            -7
          )
        };
      }


      case 'MES': {

        const desde = new Date(
          hoy.getFullYear(),
          hoy.getMonth(),
          1
        );

        // Mismo día del mes anterior.
        // Se ajusta al último día disponible cuando
        // el mes anterior tiene menos días.
        const ultimoDiaMesAnterior = new Date(
          hoy.getFullYear(),
          hoy.getMonth(),
          0
        ).getDate();

        const hastaComparacion = new Date(
          hoy.getFullYear(),
          hoy.getMonth() - 1,
          Math.min(
            hoy.getDate(),
            ultimoDiaMesAnterior
          )
        );

        const desdeComparacion = new Date(
          hoy.getFullYear(),
          hoy.getMonth() - 1,
          1
        );

        return {
          desde,
          hasta: hoy,
          desdeComparacion,
          hastaComparacion
        };
      }


      case 'ANIO': {

        const desde = new Date(
          hoy.getFullYear(),
          0,
          1
        );

        const anioAnterior = hoy.getFullYear() - 1;

        const ultimoDiaMesComparacion = new Date(
          anioAnterior,
          hoy.getMonth() + 1,
          0
        ).getDate();

        const hastaComparacion = new Date(
          anioAnterior,
          hoy.getMonth(),
          Math.min(
            hoy.getDate(),
            ultimoDiaMesComparacion
          )
        );

        const desdeComparacion = new Date(
          anioAnterior,
          0,
          1
        );

        return {
          desde,
          hasta: hoy,
          desdeComparacion,
          hastaComparacion
        };
      }


      case 'PERSONALIZADO':
        return this.obtenerRangoPersonalizado();

    }
  }


  private obtenerRangoPersonalizado(): RangoFechasDashboard | null {

    if (
      !this.fechaDesdePersonalizada ||
      !this.fechaHastaPersonalizada
    ) {

      this.errorFiltroFechas =
        'Selecciona la fecha inicial y la fecha final.';

      return null;
    }

    const desde = this.parsearFechaLocal(
      this.fechaDesdePersonalizada
    );

    const hasta = this.parsearFechaLocal(
      this.fechaHastaPersonalizada
    );

    if (!desde || !hasta) {

      this.errorFiltroFechas =
        'Las fechas seleccionadas no son válidas.';

      return null;
    }

    if (desde.getTime() > hasta.getTime()) {

      this.errorFiltroFechas =
        'La fecha inicial no puede ser posterior a la fecha final.';

      return null;
    }

    const hoy = this.obtenerHoyLocal();

    if (hasta.getTime() > hoy.getTime()) {

      this.errorFiltroFechas =
        'El periodo personalizado no puede incluir fechas futuras.';

      return null;
    }

    // La comparación utiliza un periodo anterior
    // con exactamente la misma cantidad de días.
    const diasPeriodo = this.calcularDiasInclusivos(
      desde,
      hasta
    );

    const desdeComparacion = this.agregarDias(
      desde,
      -diasPeriodo
    );

    const hastaComparacion = this.agregarDias(
      desde,
      -1
    );

    return {
      desde,
      hasta,
      desdeComparacion,
      hastaComparacion
    };
  }


  // ============================================================
  // FUNCIONES AUXILIARES PARA FECHAS
  // ============================================================

  private obtenerHoyLocal(): Date {

    const ahora = new Date();

    return new Date(
      ahora.getFullYear(),
      ahora.getMonth(),
      ahora.getDate()
    );
  }


  private agregarDias(
    fecha: Date,
    dias: number
  ): Date {

    return new Date(
      fecha.getFullYear(),
      fecha.getMonth(),
      fecha.getDate() + dias
    );
  }


  /**
   * Calcula días calendario usando los componentes de fecha
   * y UTC únicamente para la resta matemática.
   *
   * No convierte las fechas locales con toISOString().
   */
  private calcularDiasInclusivos(
    desde: Date,
    hasta: Date
  ): number {

    const inicioUTC = Date.UTC(
      desde.getFullYear(),
      desde.getMonth(),
      desde.getDate()
    );

    const finUTC = Date.UTC(
      hasta.getFullYear(),
      hasta.getMonth(),
      hasta.getDate()
    );

    return Math.floor(
      (finUTC - inicioUTC) / 86400000
    ) + 1;
  }


  private parsearFechaLocal(valor: string): Date | null {

    const coincidencia = /^(\d{4})-(\d{2})-(\d{2})$/.exec(
      valor
    );

    if (!coincidencia) {
      return null;
    }

    const anio = Number(coincidencia[1]);
    const mes = Number(coincidencia[2]);
    const dia = Number(coincidencia[3]);

    const fecha = new Date(
      anio,
      mes - 1,
      dia
    );

    // Evitar que fechas inexistentes se normalicen automáticamente.
    if (
      fecha.getFullYear() !== anio ||
      fecha.getMonth() !== mes - 1 ||
      fecha.getDate() !== dia
    ) {
      return null;
    }

    return fecha;
  }


  /**
   * Formato YYYY-MM-DD utilizando hora local.
   * Es el formato que espera el endpoint de Spring Boot.
   */
  private formatearFechaLocal(fecha: Date): string {

    const anio = fecha.getFullYear();

    const mes = String(
      fecha.getMonth() + 1
    ).padStart(2, '0');

    const dia = String(
      fecha.getDate()
    ).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
  }


  get fechaMaxima(): string {

    return this.formatearFechaLocal(
      this.obtenerHoyLocal()
    );
  }


  // ============================================================
  // RESUMEN DE INVENTARIO
  // ============================================================

  cargarResumenInventario(): void {

    if (!this.puedeConsultarInventario) {
      return;
    }

    this.cancelarSolicitudInventario();

    const requestVersion = this.inventarioRequestVersion;

    this.resumenInventario = null;
    this.productosStockBajo = [];
    this.errorInventario = '';

    this.cargandoInventario = true;
    this.cdr.markForCheck();

    this.inventarioSubscription = this.dashboardService
      .obtenerResumenInventario(this.diasAnticipacion)
      .pipe(
        finalize(() => {
          if (requestVersion === this.inventarioRequestVersion) {

            this.cargandoInventario = false;
            this.inventarioSubscription = null;

            this.cdr.markForCheck();
          }
        })
      )
      .subscribe({

        next: (respuesta) => {

          if (requestVersion !== this.inventarioRequestVersion) {
            return;
          }

          this.resumenInventario = respuesta;

          // Adaptación temporal para que la plantilla actual
          // pueda mostrar las alertas del endpoint consolidado.
          this.productosStockBajo = this.crearVistaAlertasStock(respuesta);
          this.cdr.markForCheck();
        },

        error: (error: unknown) => {

          if (requestVersion !== this.inventarioRequestVersion) {
            return;
          }

          console.error(
            'Error al cargar el resumen de inventario:',
            error
          );

          this.resumenInventario = null;
          this.productosStockBajo = [];

          this.errorInventario = this.obtenerMensajeError(
            error,
            'No se pudo cargar el resumen de inventario.'
          );

          this.cdr.markForCheck();
        }

      });
  }


  cambiarDiasAnticipacion(dias: number): void {

    if (
      dias !== 7 &&
      dias !== 15 &&
      dias !== 30
    ) {
      return;
    }

    if (this.diasAnticipacion === dias) {
      return;
    }

    this.diasAnticipacion = dias;

    this.cargarResumenInventario();
  }


  /**
   * Convierte las dos listas del backend en una estructura
   * temporal para la plantilla anterior.
   *
   * No vuelve a calcular el stock ni los mínimos:
   * utiliza directamente los resultados del backend.
   */
  private crearVistaAlertasStock(
    inventario: DashboardInventarioResponse
  ): ProductoAlertaDashboardVista[] {

    const productos = new Map<
      number,
      ProductoAlertaDashboardVista
    >();

    const agregarPresentacion = (
      alerta: DashboardPresentacionAlerta,
      estadoAlerta: EstadoAlertaStock
    ): void => {

      let producto = productos.get(alerta.idProducto);

      if (!producto) {

        producto = {
          id: alerta.idProducto,
          nombre: alerta.nombreProducto,
          presentaciones: []
        };

        productos.set(
          alerta.idProducto,
          producto
        );
      }

      producto.presentaciones.push({
        id: alerta.idPresentacion,
        tipo: alerta.tipoPresentacion,
        factorConversion: alerta.factorConversion,
        stockDisponible: alerta.stockDisponible,
        stockMinimo: alerta.stockMinimo,
        estadoAlerta
      });
    };


    for (const alerta of inventario.presentacionesAgotadas ?? []) {

      agregarPresentacion(
        alerta,
        'AGOTADO'
      );
    }


    for (const alerta of inventario.presentacionesPorReponer ?? []) {

      agregarPresentacion(
        alerta,
        'REPONER'
      );
    }


    return Array.from(productos.values());
  }


  // ============================================================
  // COMPATIBILIDAD TEMPORAL CON LA PLANTILLA ACTUAL
  // ============================================================

  get puedeConsultarAlertasStock(): boolean {

    return this.puedeConsultarInventario;
  }


  get cargandoAlertasStock(): boolean {

    return this.cargandoInventario;
  }


  get errorAlertasStock(): string {

    return this.errorInventario;
  }


  cargarAlertasStockBajo(): void {

    this.cargarResumenInventario();
  }


  presentacionesConStockBajo(
    producto: ProductoAlertaDashboardVista
  ): PresentacionAlertaDashboardVista[] {

    return producto.presentaciones;
  }


  get totalAlertasStockBajo(): number {

    return this.productosStockBajo.reduce(
      (total, producto) =>
        total + producto.presentaciones.length,
      0
    );
  }


  etiquetaTipo(tipo: TipoPresentacion): string {

    const nombres: Record<TipoPresentacion, string> = {
      UNIDAD: 'Unidad',
      BLISTER: 'Blíster',
      CAJA: 'Caja',
      FRASCO: 'Frasco',
      TIRA: 'Tira',
      OTRO: 'Otro'
    };

    return nombres[tipo] || tipo;
  }

  // ============================================================
  // FORMATO DE FECHAS PARA EL DASHBOARD
  // ============================================================

  formatearFechaCorta(
    fecha: string | null | undefined
  ): string {

    if (!fecha) {
      return 'No disponible';
    }

    // Las fechas del backend llegan como YYYY-MM-DD.
    // Se separan sus componentes para evitar conversiones UTC.
    const partes = fecha.substring(0, 10).split('-');

    if (partes.length !== 3) {
      return 'No disponible';
    }

    const [anio, mes, dia] = partes;

    if (!anio || !mes || !dia) {
      return 'No disponible';
    }

    return `${dia}/${mes}/${anio}`;
  }


  descripcionVencimiento(dias: number): string {

    if (dias < 0) {

      const diasVencidos = Math.abs(dias);

      return diasVencidos === 1
        ? 'Vencido hace 1 día'
        : `Vencido hace ${diasVencidos} días`;
    }

    if (dias === 0) {
      return 'Vence hoy';
    }

    return dias === 1
      ? 'Vence en 1 día'
      : `Vence en ${dias} días`;
  }


  // ============================================================
  // MOVIMIENTOS RECIENTES
  // ============================================================

  cargarMovimientosRecientes(): void {

    if (!this.puedeConsultarMovimientos) {
      return;
    }

    this.cancelarSolicitudMovimientos();

    const requestVersion = this.movimientosRequestVersion;

    this.movimientosRecientes = [];
    this.errorMovimientos = '';

    this.cargandoMovimientos = true;
    this.cdr.markForCheck();

    this.movimientosSubscription = this.dashboardService
      .listarMovimientosRecientes()
      .pipe(
        finalize(() => {
          if (requestVersion === this.movimientosRequestVersion) {

            this.cargandoMovimientos = false;
            this.movimientosSubscription = null;

            this.cdr.markForCheck();
          }
        })
      )
      .subscribe({

        next: (respuesta) => {

          if (requestVersion !== this.movimientosRequestVersion) {
            return;
          }

          this.movimientosRecientes = respuesta ?? [];
          this.cdr.markForCheck();
        },

        error: (error: unknown) => {

          if (requestVersion !== this.movimientosRequestVersion) {
            return;
          }

          console.error(
            'Error al cargar los movimientos recientes:',
            error
          );

          this.movimientosRecientes = [];

          this.errorMovimientos = this.obtenerMensajeError(
            error,
            'No se pudieron cargar los movimientos recientes.'
          );

          this.cdr.markForCheck();
        }

      });
  }


  // ============================================================
  // CANCELACIÓN DE SOLICITUDES
  // ============================================================

  private cancelarSolicitudVentas(): void {

    this.ventasRequestVersion++;

    this.ventasSubscription?.unsubscribe();
    this.ventasSubscription = null;

    this.cargandoVentas = false;
  }


  private cancelarSolicitudInventario(): void {

    this.inventarioRequestVersion++;

    this.inventarioSubscription?.unsubscribe();
    this.inventarioSubscription = null;

    this.cargandoInventario = false;
  }


  private cancelarSolicitudMovimientos(): void {

    this.movimientosRequestVersion++;

    this.movimientosSubscription?.unsubscribe();
    this.movimientosSubscription = null;

    this.cargandoMovimientos = false;
  }


  // ============================================================
  // LIMPIAR SECCIONES CUANDO SE RETIRA UN PERMISO
  // ============================================================

  private limpiarResumenVentas(): void {

    this.cancelarSolicitudVentas();

    this.resumenVentas = null;
    this.errorVentas = '';
    this.errorFiltroFechas = '';
    this.cdr.markForCheck();
  }


  private limpiarResumenInventario(): void {

    this.cancelarSolicitudInventario();

    this.resumenInventario = null;
    this.productosStockBajo = [];
    this.errorInventario = '';
    this.cdr.markForCheck();
  }


  private limpiarMovimientosRecientes(): void {

    this.cancelarSolicitudMovimientos();

    this.movimientosRecientes = [];
    this.errorMovimientos = '';
    this.cdr.markForCheck();
  }


  // ============================================================
  // MENSAJES DE ERROR
  // ============================================================

  private obtenerMensajeError(
    error: unknown,
    mensajePredeterminado: string
  ): string {

    if (!(error instanceof HttpErrorResponse)) {
      return mensajePredeterminado;
    }

    if (error.status === 403) {

      const cuerpo = error.error as {
        mensaje?: unknown;
      } | null;

      return typeof cuerpo?.mensaje === 'string'
        ? cuerpo.mensaje
        : 'No tiene permisos para consultar esta información.';
    }


    if (error.status === 0) {

      return 'No se pudo conectar con el servidor.';
    }


    const cuerpo = error.error as {
      mensaje?: unknown;
      message?: unknown;
    } | null;

    if (typeof cuerpo?.mensaje === 'string') {
      return cuerpo.mensaje;
    }

    if (typeof cuerpo?.message === 'string') {
      return cuerpo.message;
    }

    return mensajePredeterminado;
  }


  // ============================================================
  // ACCESO A OTROS MÓDULOS
  // Se conservan para las tarjetas de la plantilla actual.
  // ============================================================

  get puedeVerProductos(): boolean {

    return this.authService.hasPermission(
      'PRODUCTO_ACCESO'
    );
  }


  get puedeRegistrarVenta(): boolean {

    return this.authService.hasPermission(
      'PUNTO_VENTA_ACCESO'
    );
  }


  get puedeVerClientes(): boolean {

    return this.authService.hasPermission(
      'CLIENTE_ACCESO'
    );
  }


  get mostrarTarjetas(): boolean {

    return this.puedeVerProductos
      || this.puedeRegistrarVenta
      || this.puedeVerClientes;
  }

}


