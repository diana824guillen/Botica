import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PerfilModal } from './perfil-modal';

describe('PerfilModal', () => {
  let component: PerfilModal;
  let fixture: ComponentFixture<PerfilModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilModal],
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
