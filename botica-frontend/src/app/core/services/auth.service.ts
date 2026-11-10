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

  // Mostrar el aviso dos minutos antes del vencimiento.
  private readonly TIEMPO_AVISO_MS = 2 * 60 * 1000;

  // Claves utilizadas para controlar la sesión.
  private readonly CLAVE_SESION_EXPIRADA =
    'vida-salud:session-expired';

  private readonly CLAVE_USUARIO_EXPIRADO =
    'vida-salud:expired-user';

  private readonly PREFIJO_BORRADOR =
    'vida-salud:pos-draft:';

  // ============================================================
  // ESTADOS REACTIVOS
  // ============================================================

  private sessionWarningSubject =
    new BehaviorSubject<boolean>(false);

  readonly sessionWarning$ =
    this.sessionWarningSubject.asObservable();

  private remainingSecondsSubject =
    new BehaviorSubject<number>(0);

  readonly sessionSecondsRemaining$ =
    this.remainingSecondsSubject.asObservable();

  private sessionClosingSubject =
    new BehaviorSubject<boolean>(false);

  readonly sessionClosing$ =
    this.sessionClosingSubject.asObservable();

  // ============================================================
  // TEMPORIZADORES
  // ============================================================

  private warningTimeout:
    ReturnType<typeof setTimeout> | null = null;

  private expirationTimeout:
    ReturnType<typeof setTimeout> | null = null;

  private countdownInterval:
    ReturnType<typeof setInterval> | null = null;

  private tokenMonitoreado: string | null = null;

  private fechaExpiracion = 0;

  // ============================================================
  // CONTROL DE SOLICITUDES ACTIVAS
  // ============================================================

  // Se contabilizan por token para no mezclar solicitudes
  // de una sesión anterior con una nueva autenticación.
  private solicitudesActivas = new Map<string, number>();

  private tokenCierrePendiente: string | null = null;

  private redireccionando = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {

    // Recuperar el monitoreo si la página se actualiza.
    const token = localStorage.getItem('token');

    if (token) {

      const expiracion = this.obtenerFechaExpiracion(token);

      if (expiracion && expiracion > Date.now()) {

        this.programarMonitoreo(token, expiracion);

      } else {

        this.solicitarExpiracionSesion(token);
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

          // Identificar al usuario de la sesión anterior.
          const usuarioAnterior = sessionStorage.getItem(
            this.CLAVE_USUARIO_EXPIRADO
          );

          // No permitir recuperar el borrador de otro usuario.
          if (
            usuarioAnterior &&
            usuarioAnterior.toLowerCase() !==
              response.username.toLowerCase()
          ) {

            this.eliminarBorradorVenta(usuarioAnterior);
          }

          // Guardar los datos de la nueva sesión.
          localStorage.setItem('token', response.token);

          localStorage.setItem(
            'user',
            JSON.stringify(response)
          );

          // Restablecer el control de la sesión.
          this.tokenCierrePendiente = null;
          this.redireccionando = false;

          this.sessionClosingSubject.next(false);

          // Programar aviso y expiración.
          this.programarMonitoreo(response.token);
        })
      );
  }

  // ============================================================
  // CERRAR SESIÓN MANUALMENTE
  // ============================================================

  logout(): void {

    const username = this.obtenerUsuarioActual();

    // Un cierre voluntario descarta el borrador pendiente.
    if (username) {
      this.eliminarBorradorVenta(username);
    }

    sessionStorage.removeItem(
      this.CLAVE_SESION_EXPIRADA
    );

    sessionStorage.removeItem(
      this.CLAVE_USUARIO_EXPIRADO
    );

    this.tokenCierrePendiente = null;

    this.limpiarDatosAutenticacion();

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

      this.solicitarExpiracionSesion(token);

      return false;
    }

    if (this.tokenCierrePendiente === token) {
      return false;
    }

    // Garantizar que el token tenga un monitoreo activo.
    if (this.tokenMonitoreado !== token) {

      this.programarMonitoreo(token, expiracion);
    }

    return true;
  }

  // ============================================================
  // PROGRAMAR AVISO Y EXPIRACIÓN
  // ============================================================

  private programarMonitoreo(
    token: string,
    expiracionConocida?: number
  ): void {

    // No programar un token que ya fue reemplazado.
    if (localStorage.getItem('token') !== token) {
      return;
    }

    const expiracion =
      expiracionConocida ??
      this.obtenerFechaExpiracion(token);

    if (!expiracion || expiracion <= Date.now()) {

      this.solicitarExpiracionSesion(token);

      return;
    }

    // Evitar duplicar temporizadores.
    if (
      this.tokenMonitoreado === token &&
      this.expirationTimeout !== null
    ) {
      return;
    }

    this.limpiarTemporizadores();

    this.tokenMonitoreado = token;
    this.fechaExpiracion = expiracion;

    this.tokenCierrePendiente = null;

    this.sessionWarningSubject.next(false);
    this.sessionClosingSubject.next(false);
    this.remainingSecondsSubject.next(0);

    const tiempoRestante = expiracion - Date.now();

    const tiempoParaAviso = Math.max(
      0,
      tiempoRestante - this.TIEMPO_AVISO_MS
    );

    // AVISO PREVIO.
    this.warningTimeout = setTimeout(() => {

      this.warningTimeout = null;

      if (localStorage.getItem('token') !== token) {
        return;
      }

      this.sessionWarningSubject.next(true);

      this.actualizarCuentaRegresiva(token);

      this.countdownInterval = setInterval(() => {

        this.actualizarCuentaRegresiva(token);

      }, 1000);

    }, tiempoParaAviso);

    // EXPIRACIÓN DEFINITIVA.
    this.expirationTimeout = setTimeout(() => {

      this.expirationTimeout = null;

      this.solicitarExpiracionSesion(token);

    }, tiempoRestante);
  }

  private actualizarCuentaRegresiva(token: string): void {

    if (localStorage.getItem('token') !== token) {
      return;
    }

    const segundos = Math.max(
      0,
      Math.ceil(
        (this.fechaExpiracion - Date.now()) / 1000
      )
    );

    this.remainingSecondsSubject.next(segundos);
  }

  // Ocultar el aviso, pero no prolongar la sesión.
  ocultarAvisoSesion(): void {

    // Si está esperando una operación, mantener el aviso.
    if (this.tokenCierrePendiente) {
      return;
    }

    this.sessionWarningSubject.next(false);
  }

  // ============================================================
  // CONTROL DE SOLICITUDES
  // ============================================================

  // Se ejecuta antes de iniciar una solicitud protegida. No permite comenzar operaciones con un token vencido.
  iniciarSolicitudProtegida(token: string): boolean {

    if (!token || localStorage.getItem('token') !== token) {
      return false;
    }

    if (this.tokenCierrePendiente === token) {
      return false;
    }

    const expiracion = this.obtenerFechaExpiracion(token);

    if (!expiracion || Date.now() >= expiracion) {

      this.solicitarExpiracionSesion(token);

      return false;
    }

    const actual = this.solicitudesActivas.get(token) ?? 0;

    this.solicitudesActivas.set(
      token,
      actual + 1
    );

    return true;
  }

  // Se ejecuta cuando termina una solicitud protegida.
  finalizarSolicitudProtegida(token: string): void {

    const actual = this.solicitudesActivas.get(token) ?? 0;

    if (actual <= 1) {

      this.solicitudesActivas.delete(token);

    } else {

      this.solicitudesActivas.set(
        token,
        actual - 1
      );
    }

    // Si el token venció mientras había solicitudes activas, comprobar si ya se pueden cerrar todos los accesos.
    if (this.tokenCierrePendiente === token) {

      this.procesarExpiracionSiCorresponde(token);
    }
  }

  // Se ejecuta cuando el backend responde HTTP 401.
  solicitarCierrePorAutenticacion(token: string): void {
    this.solicitarExpiracionSesion(token);
  }

  hayOperacionesPendientesDeCierre(): boolean {
    const token = this.tokenCierrePendiente;

    return token !== null &&
      (this.solicitudesActivas.get(token) ?? 0) > 0;
  }

  // ============================================================
  // SOLICITAR EXPIRACIÓN
  // ============================================================

  private solicitarExpiracionSesion(token: string): void {

    // Ignorar respuestas de una sesión anterior.
    if (localStorage.getItem('token') !== token) {
      return;
    }

    this.tokenCierrePendiente = token;

    this.registrarSesionExpirada();

    this.sessionClosingSubject.next(true);
    this.sessionWarningSubject.next(true);
    this.remainingSecondsSubject.next(0);

    this.procesarExpiracionSiCorresponde(token);
  }

  // Esperar a que terminen las solicitudes que comenzaron mientras el token todavía era válido.
  private procesarExpiracionSiCorresponde(token: string): void {

    if (this.tokenCierrePendiente !== token) {
      return;
    }

    if (localStorage.getItem('token') !== token) {
      return;
    }

    const pendientes = this.solicitudesActivas.get(token) ?? 0;

    // No cerrar mientras exista una solicitud en ejecución.
    if (pendientes > 0) {
      return;
    }

    if (this.redireccionando) {
      return;
    }

    this.redireccionando = true;

    // Mantener registrado que la sesión terminó por vencimiento.
    this.registrarSesionExpirada();

    // El borrador del POS no se elimina.
    this.tokenCierrePendiente = null;

    this.limpiarDatosAutenticacion(token);

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
  // LIMPIAR DATOS DE AUTENTICACIÓN
  // ============================================================

  private limpiarDatosAutenticacion(
    tokenEsperado?: string
  ): void {

    // Evitar que una sesión anterior elimine una autenticación nueva.
    if (
      tokenEsperado &&
      localStorage.getItem('token') !== tokenEsperado
    ) {
      return;
    }

    localStorage.removeItem('token');
    localStorage.removeItem('user');

    this.limpiarTemporizadores();

    this.tokenMonitoreado = null;
    this.fechaExpiracion = 0;

    this.sessionWarningSubject.next(false);
    this.remainingSecondsSubject.next(0);
    this.sessionClosingSubject.next(false);
  }

  private limpiarTemporizadores(): void {

    if (this.warningTimeout !== null) {

      clearTimeout(this.warningTimeout);

      this.warningTimeout = null;
    }

    if (this.expirationTimeout !== null) {

      clearTimeout(this.expirationTimeout);

      this.expirationTimeout = null;
    }

    if (this.countdownInterval !== null) {

      clearInterval(this.countdownInterval);

      this.countdownInterval = null;
    }
  }

  // ============================================================
  // OBTENER FECHA DE EXPIRACIÓN DEL JWT
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

    if (!user) {
      return '';
    }

    try {

      return JSON.parse(user).perfil || '';

    } catch {

      return '';
    }
  }

  getUserPermissions(): string[] {

    const user = localStorage.getItem('user');

    if (!user) {
      return [];
    }

    try {

      const permisos = JSON.parse(user).permisos;

      return Array.isArray(permisos)
        ? permisos.filter(
            (permiso: unknown): permiso is string =>
              typeof permiso === 'string'
          )
        : [];

    } catch {

      return [];
    }
  }

  hasPermission(permission: string): boolean {

    if (this.getUserRole() === 'ADMIN') {
      return true;
    }

    return this.getUserPermissions().includes(permission);
  }

  hasAnyPermission(
    requiredPermissions: string | string[]
  ): boolean {

    if (this.getUserRole() === 'ADMIN') {
      return true;
    }

    const requeridos = Array.isArray(requiredPermissions)
      ? requiredPermissions
      : [requiredPermissions];

    const permisosUsuario = this.getUserPermissions();

    return requeridos.some(permiso =>
      permisosUsuario.includes(permiso)
    );
  }

  // ============================================================
  // RUTA DESPUÉS DE INICIAR SESIÓN
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

    sessionStorage.removeItem(
      this.CLAVE_SESION_EXPIRADA
    );

    sessionStorage.removeItem(
      this.CLAVE_USUARIO_EXPIRADO
    );

    if (fueExpirada && mismoUsuario && existeBorrador) {
      return '/ventas';
    }

    return '/dashboard';
  }

  // ============================================================
  // CONSERVAR BORRADOR DEL POS
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

    const usuario =
      username ?? this.obtenerUsuarioActual();

    if (!usuario) {
      return;
    }

    sessionStorage.removeItem(
      this.obtenerClaveBorrador(usuario)
    );
  }

}


