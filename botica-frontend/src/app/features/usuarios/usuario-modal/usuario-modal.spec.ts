import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsuarioModal } from './usuario-modal';

describe('UsuarioModal', () => {
  let component: UsuarioModal;
  let fixture: ComponentFixture<UsuarioModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsuarioModal],
    }).compileComponents();

    fixture = TestBed.createComponent(UsuarioModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
