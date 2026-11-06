import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ButtonComponent } from '../../../shared/components/button/button';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonComponent
  ],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {

  loginForm: FormGroup;

  errorMessage = '';

  isLoading = false;

  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {

    this.loginForm = this.fb.group({

      username: [
        '',
        [
          Validators.required,
          Validators.pattern('^[a-zA-Z0-9]{4,20}$')
        ]
      ],

      password: [
        '',
        Validators.required
      ]

    });

    // Mostrar aviso cuando se llega al login por vencimiento.
    const sessionExpired =
      this.route.snapshot.queryParamMap.get('sessionExpired');

    if (sessionExpired === 'true') {

      this.errorMessage =
        'Tu sesión ha vencido. Inicia sesión nuevamente para continuar.';
    }
  }

  togglePasswordVisibility(): void {

    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {

    if (this.loginForm.invalid) {

      this.loginForm.markAllAsTouched();

      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.cdr.detectChanges();

    this.authService
      .login(this.loginForm.value)
      .subscribe({

        // Inicio de sesión correcto.
        next: response => {

          // Volver al POS únicamente si existe un borrador de la misma usuaria que acaba de autenticarse.
          const ruta = this.authService.obtenerRutaDespuesLogin(
            response.username
          );

          void this.router.navigateByUrl(ruta);
        },

        // Error de autenticación.
        error: err => {

          this.isLoading = false;

          if (err.error && err.error.mensaje) {

            this.errorMessage = err.error.mensaje;

          } else {

            this.errorMessage =
              'Error de conexión con el servidor.';
          }

          this.cdr.detectChanges();
        }

      });
  }
}


