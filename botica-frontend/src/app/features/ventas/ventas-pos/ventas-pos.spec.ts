import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VentasPos } from './ventas-pos';

describe('VentasPos', () => {
  let component: VentasPos;
  let fixture: ComponentFixture<VentasPos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VentasPos],
    }).compileComponents();

    fixture = TestBed.createComponent(VentasPos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
