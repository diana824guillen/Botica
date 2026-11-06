import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import {
  BehaviorSubject,
  Observable,
  tap
} from 'rxjs';

import { environment } from '../../../environments/environment';

export interface LoginResponse {
  token: string;
  username: string;
  perfil: string;
  tipoDocumento: string;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  permisos: string[];
}

interface JwtPayload {
  exp?: number;
}

interface BorradorGuardado<T> {
  username: string;
  data: T;
  updatedAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = `${environment.apiUrl}/auth`;

  // Tiempo de anticipación del aviso: 2 minutos.
  private readonly TIEMPO_AVISO_MS = 2 * 60 * 1000;

  // Claves de almacenamiento.
  private readonly CLAVE_SESION_EXPIRADA = 'vida-salud:session-expired';

  private readonly CLAVE_USUARIO_EXPIRADO = 'vida-salud:expired-user';

  private readonly PREFIJO_BORRADOR = 'vida-salud:pos-draft:';

  // Estado del aviso de expiración.
  private sessionWarningSubject = new BehaviorSubject<boolean>(false);

  readonly sessionWarning$ =
    this.sessionWarningSubject.asObservable();

  // Segundos que faltan para vencer el token.
  private remainingSecondsSubject = new BehaviorSubject<number>(0);

  readonly sessionSecondsRemaining$ =
    this.remainingSecondsSubject.asObservable();

  // Temporizadores.
  private warningTimeout: ReturnType<typeof setTimeout> | null = null;

  private expirationTimeout: ReturnType<typeof setTimeout> | null = null;

  private countdownInterval: ReturnType<typeof setInterval> | null = null;

  private tokenMonitoreado: string | null = null;

  private fechaExpiracion = 0;

  // Control de solicitudes que ya están en curso.
  private solicitudesActivas = 0;

  private expiracionPendiente = false;

  private redireccionando = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {

    // Recuperar la programación del token si se actualiza la página.
    const token = localStorage.getItem('token');

    if (token) {

      const expiracion = this.obtenerFechaExpiracion(token);

      if (expiracion && expiracion > Date.now()) {
        this.programarMonitoreo(token, expiracion);
      }
    }
  }

  // ============================================================
  // INICIAR SESIÓN
  // ============================================================

  login(credentials: any): Observable<LoginResponse> {

    return this.http
      .post<LoginResponse>(
        `${this.apiUrl}/login`,
        credentials
      )
      .pipe(
        tap(response => {

          // Si inició sesión otra persona después del vencimiento, eliminar el borrador de la usuaria anterior.
          const usuarioAnterior = sessionStorage.getItem(
            this.CLAVE_USUARIO_EXPIRADO
          );

          if (
            usuarioAnterior &&
            usuarioAnterior.toLowerCase() !==
              response.username.toLowerCase()
          ) {

            this.eliminarBorradorVenta(usuarioAnterior);
          }

          localStorage.setItem('token', response.token);

          localStorage.setItem(
            'user',
            JSON.stringify(response)
          );

          this.expiracionPendiente = false;
          this.redireccionando = false;

          // Programar aviso y cierre de sesión.
          this.programarMonitoreo(response.token);
        })
      );
  }

  // ============================================================
  // CERRAR SESIÓN MANUALMENTE
  // ============================================================

  logout(): void {

    const username = this.obtenerUsuarioActual();

    // Si la usuaria cierra sesión voluntariamente, descartar su borrador pendiente.
    if (username) {
      this.eliminarBorradorVenta(username);
    }

    sessionStorage.removeItem(this.CLAVE_SESION_EXPIRADA);
    sessionStorage.removeItem(this.CLAVE_USUARIO_EXPIRADO);

    this.limpiarDatosAutenticacion();

    this.expiracionPendiente = false;
  }

  // ============================================================
  // COMPROBAR SESIÓN
  // ============================================================

  isLoggedIn(): boolean {

    const token = localStorage.getItem('token');

    if (!token) {
      return false;
    }

    const expiracion = this.obtenerFechaExpiracion(token);

    if (!expiracion || Date.now() >= expiracion) {

      this.registrarSesionExpirada();

      this.expiracionPendiente = true;

      this.procesarExpiracionSiCorresponde();

      return false;
    }

    // Asegurar que exista un monitoreo para el token actual.
    if (this.tokenMonitoreado !== token) {
      this.programarMonitoreo(token, expiracion);
    }

    return true;
  }

  // ============================================================
  // MONITOREO DE EXPIRACIÓN
  // ============================================================

  private programarMonitoreo(
    token: string,
    expiracionConocida?: number
  ): void {

    const expiracion =
      expiracionConocida ?? this.obtenerFechaExpiracion(token);

    if (!expiracion) {

      this.solicitarExpiracionSesion();

      return;
    }

    if (expiracion <= Date.now()) {

      this.solicitarExpiracionSesion();

      return;
    }

    // Evitar programar dos veces el mismo token.
    if (
      this.tokenMonitoreado === token &&
      this.expirationTimeout !== null
    ) {
      return;
    }

    this.limpiarTemporizadores();

    this.tokenMonitoreado = token;
    this.fechaExpiracion = expiracion;

    this.sessionWarningSubject.next(false);
    this.remainingSecondsSubject.next(0);

    this.expiracionPendiente = false;

    const tiempoRestante = expiracion - Date.now();

    // Mostrar aviso dos minutos antes del vencimiento.
    const tiempoParaAviso = Math.max(
      0,
      tiempoRestante - this.TIEMPO_AVISO_MS
    );

    this.warningTimeout = setTimeout(() => {

      this.sessionWarningSubject.next(true);

      this.actualizarCuentaRegresiva();

      this.countdownInterval = setInterval(() => {

        this.actualizarCuentaRegresiva();

      }, 1000);

    }, tiempoParaAviso);

    // Programar el vencimiento.
    this.expirationTimeout = setTimeout(() => {

      this.solicitarExpiracionSesion();

    }, tiempoRestante);
  }

  private actualizarCuentaRegresiva(): void {

    const segundos = Math.max(
      0,
      Math.ceil((this.fechaExpiracion - Date.now()) / 1000)
    );

    this.remainingSecondsSubject.next(segundos);
  }

  // Ocultar el aviso sin prolongar la sesión.
  ocultarAvisoSesion(): void {

    this.sessionWarningSubject.next(false);
  }

  // ============================================================
  // CONTROL DE SOLICITUDES EN CURSO
  // ============================================================

  // Se llama cuando comienza una solicitud protegida con JWT.
  iniciarSolicitudProtegida(): void {

    this.solicitudesActivas++;
  }

  // Se llama cuando termina una solicitud protegida.
  finalizarSolicitudProtegida(): void {

    this.solicitudesActivas = Math.max(
      0,
      this.solicitudesActivas - 1
    );

    this.procesarExpiracionSiCorresponde();
  }

  // Se llama cuando el backend responde 401.
  solicitarCierrePorAutenticacion(): void {

    this.solicitarExpiracionSesion();
  }

  private solicitarExpiracionSesion(): void {

    // No hacer nada si la sesión ya fue cerrada manualmente.
    if (!localStorage.getItem('token')) {
      return;
    }

    this.registrarSesionExpirada();

    this.expiracionPendiente = true;

    this.procesarExpiracionSiCorresponde();
  }

  // No cerrar la sesión mientras existan peticiones iniciadas.
  private procesarExpiracionSiCorresponde(): void {

    if (!this.expiracionPendiente) {
      return;
    }

    if (this.solicitudesActivas > 0) {
      return;
    }

    if (this.redireccionando) {
      return;
    }

    this.redireccionando = true;

    // El borrador del POS no se elimina.
    this.registrarSesionExpirada();

    this.limpiarDatosAutenticacion();

    this.expiracionPendiente = false;

    void this.router.navigate(
      ['/login'],
      {
        queryParams: {
          sessionExpired: 'true'
        }
      }
    ).finally(() => {

      this.redireccionando = false;

    });
  }

  // ============================================================
  // REGISTRAR EXPIRACIÓN
  // ============================================================

  private registrarSesionExpirada(): void {

    const username = this.obtenerUsuarioActual();

    sessionStorage.setItem(
      this.CLAVE_SESION_EXPIRADA,
      'true'
    );

    if (username) {

      sessionStorage.setItem(
        this.CLAVE_USUARIO_EXPIRADO,
        username
      );
    }
  }

  sesionExpirada(): boolean {

    return sessionStorage.getItem(
      this.CLAVE_SESION_EXPIRADA
    ) === 'true';
  }

  // ============================================================
  // LIMPIAR AUTENTICACIÓN
  // ============================================================

  private limpiarDatosAutenticacion(): void {

    localStorage.removeItem('token');
    localStorage.removeItem('user');

    this.limpiarTemporizadores();

    this.tokenMonitoreado = null;
    this.fechaExpiracion = 0;

    this.sessionWarningSubject.next(false);
    this.remainingSecondsSubject.next(0);
  }

  private limpiarTemporizadores(): void {

    if (this.warningTimeout) {
      clearTimeout(this.warningTimeout);
      this.warningTimeout = null;
    }

    if (this.expirationTimeout) {
      clearTimeout(this.expirationTimeout);
      this.expirationTimeout = null;
    }

    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
  }

  // ============================================================
  // LEER EXPIRACIÓN DEL JWT
  // ============================================================

  private obtenerFechaExpiracion(
    token: string
  ): number | null {

    try {

      const partes = token.split('.');

      if (partes.length !== 3) {
        return null;
      }

      const base64 = partes[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const base64Completo = base64.padEnd(
        Math.ceil(base64.length / 4) * 4,
        '='
      );

      const binario = atob(base64Completo);

      const bytes = Uint8Array.from(
        binario,
        caracter => caracter.charCodeAt(0)
      );

      const payload: JwtPayload = JSON.parse(
        new TextDecoder().decode(bytes)
      );

      if (
        typeof payload.exp !== 'number' ||
        !Number.isFinite(payload.exp)
      ) {
        return null;
      }

      // La expiración del JWT está expresada en segundos.
      return payload.exp * 1000;

    } catch {

      return null;
    }
  }

  // ============================================================
  // DATOS DEL USUARIO
  // ============================================================

  private obtenerUsuarioActual(): string | null {

    const user = localStorage.getItem('user');

    if (!user) {
      return null;
    }

    try {

      const userData = JSON.parse(user);

      return typeof userData.username === 'string'
        ? userData.username
        : null;

    } catch {

      return null;
    }
  }

  getCurrentUsername(): string | null {

    return this.obtenerUsuarioActual();
  }

  getUserRole(): string {

    const user = localStorage.getItem('user');

    if (user) {

      try {
        return JSON.parse(user).perfil || '';
      } catch {
        return '';
      }
    }

    return '';
  }

  // ============================================================
  // RUTA DESPUÉS DE VOLVER A INICIAR SESIÓN
  // ============================================================

  obtenerRutaDespuesLogin(username: string): string {

    const fueExpirada = this.sesionExpirada();

    const usuarioExpirado = sessionStorage.getItem(
      this.CLAVE_USUARIO_EXPIRADO
    );

    const mismoUsuario =
      !!usuarioExpirado &&
      usuarioExpirado.toLowerCase() === username.toLowerCase();

    const existeBorrador =
      mismoUsuario &&
      !!sessionStorage.getItem(
        this.obtenerClaveBorrador(username)
      );

    sessionStorage.removeItem(this.CLAVE_SESION_EXPIRADA);
    sessionStorage.removeItem(this.CLAVE_USUARIO_EXPIRADO);

    // Recuperar el POS únicamente para la misma usuaria.
    if (fueExpirada && mismoUsuario && existeBorrador) {
      return '/ventas';
    }

    return '/dashboard';
  }

  // ============================================================
  // GUARDAR BORRADOR DE VENTA
  // ============================================================

  private obtenerClaveBorrador(username: string): string {

    return this.PREFIJO_BORRADOR + username.toLowerCase();
  }

  guardarBorradorVenta<T>(borrador: T): void {

    const username = this.obtenerUsuarioActual();

    if (!username) {
      return;
    }

    try {

      const contenido: BorradorGuardado<T> = {
        username,
        data: borrador,
        updatedAt: Date.now()
      };

      sessionStorage.setItem(
        this.obtenerClaveBorrador(username),
        JSON.stringify(contenido)
      );

    } catch (error) {

      console.error(
        'No se pudo conservar el borrador de la venta:',
        error
      );
    }
  }

  obtenerBorradorVenta<T>(): T | null {

    const username = this.obtenerUsuarioActual();

    if (!username) {
      return null;
    }

    const clave = this.obtenerClaveBorrador(username);

    const contenido = sessionStorage.getItem(clave);

    if (!contenido) {
      return null;
    }

    try {

      const borrador = JSON.parse(
        contenido
      ) as BorradorGuardado<T>;

      if (
        !borrador.username ||
        borrador.username.toLowerCase() !== username.toLowerCase()
      ) {
        return null;
      }

      return borrador.data ?? null;

    } catch {

      sessionStorage.removeItem(clave);

      return null;
    }
  }

  eliminarBorradorVenta(username?: string | null): void {

    const usuario = username ?? this.obtenerUsuarioActual();

    if (!usuario) {
      return;
    }

    sessionStorage.removeItem(
      this.obtenerClaveBorrador(usuario)
    );
  }
}


