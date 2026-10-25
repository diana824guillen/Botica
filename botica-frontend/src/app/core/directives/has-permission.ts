import { Directive, Input, TemplateRef, ViewContainerRef, OnInit } from '@angular/core';

@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective implements OnInit {
  @Input() appHasPermission: string | string[] = [];

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef
  ) {}

  ngOnInit() {
    const user = localStorage.getItem('user');
    if (!user) return;

    const userData = JSON.parse(user);
    const userPermissions: string[] = userData.permisos || [];
    const requiredPermissions = Array.isArray(this.appHasPermission) 
      ? this.appHasPermission 
      : [this.appHasPermission];

    // Si es ADMIN, tiene acceso a todo
    if (userData.perfil === 'ADMIN') {
      this.viewContainer.createEmbeddedView(this.templateRef);
      return;
    }

    // Verificamos si el usuario tiene el permiso
    const hasPermission = requiredPermissions.some(p => userPermissions.includes(p));

    if (hasPermission) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear(); // Lo borra del DOM
    }
  }
}


