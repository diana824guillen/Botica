import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsuarioClaveModal } from './usuario-clave-modal';

describe('UsuarioClaveModal', () => {
  let component: UsuarioClaveModal;
  let fixture: ComponentFixture<UsuarioClaveModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsuarioClaveModal],
    }).compileComponents();

    fixture = TestBed.createComponent(UsuarioClaveModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
