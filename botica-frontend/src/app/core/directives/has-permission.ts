import {
  Directive,
  Input,
  OnInit,
  TemplateRef,
  ViewContainerRef,
  inject
} from '@angular/core';

import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective implements OnInit {

  @Input() appHasPermission: string | string[] = [];

  private readonly authService = inject(AuthService);

  constructor(
    private readonly templateRef: TemplateRef<unknown>,
    private readonly viewContainer: ViewContainerRef
  ) {}

  ngOnInit(): void {

    this.viewContainer.clear();

    if (
      this.authService.hasAnyPermission(
        this.appHasPermission
      )
    ) {

      this.viewContainer.createEmbeddedView(
        this.templateRef
      );
    }
  }

}


