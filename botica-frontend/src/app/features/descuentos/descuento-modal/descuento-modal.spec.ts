import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DescuentoModal } from './descuento-modal';

describe('DescuentoModal', () => {
  let component: DescuentoModal;
  let fixture: ComponentFixture<DescuentoModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DescuentoModal],
    }).compileComponents();

    fixture = TestBed.createComponent(DescuentoModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
