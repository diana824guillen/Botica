import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PresentacionesModal } from './presentaciones-modal';

describe('PresentacionesModal', () => {
  let component: PresentacionesModal;
  let fixture: ComponentFixture<PresentacionesModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PresentacionesModal],
    }).compileComponents();

    fixture = TestBed.createComponent(PresentacionesModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
