import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PermisosModal } from './permisos-modal';

describe('PermisosModal', () => {
  let component: PermisosModal;
  let fixture: ComponentFixture<PermisosModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PermisosModal],
    }).compileComponents();

    fixture = TestBed.createComponent(PermisosModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
