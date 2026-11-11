import {
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewContainerRef,
  inject
} from '@angular/core';

import { Subscription } from 'rxjs';

import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective
  implements OnInit, OnChanges, OnDestroy {

  @Input() appHasPermission: string | string[] = [];

  private readonly authService = inject(AuthService);

  private permisosSubscription: Subscription | null = null;

  private vistaCreada = false;

  constructor(
    private readonly templateRef: TemplateRef<unknown>,
    private readonly viewContainer: ViewContainerRef
  ) {}

  ngOnInit(): void {

    // Escuchar los cambios de permisos durante la sesión.
    this.permisosSubscription =
      this.authService.permissions$.subscribe(() => {
        this.actualizarVista();
      });
  }

  ngOnChanges(): void {

    // Actualizar también si cambia el permiso solicitado.
    this.actualizarVista();
  }

  ngOnDestroy(): void {

    this.permisosSubscription?.unsubscribe();

    this.permisosSubscription = null;
  }

  private actualizarVista(): void {

    const tieneAcceso =
      this.authService.hasAnyPermission(
        this.appHasPermission
      );

    if (tieneAcceso && !this.vistaCreada) {

      this.viewContainer.createEmbeddedView(
        this.templateRef
      );

      this.vistaCreada = true;

    } else if (!tieneAcceso && this.vistaCreada) {

      this.viewContainer.clear();

      this.vistaCreada = false;
    }
  }

}


