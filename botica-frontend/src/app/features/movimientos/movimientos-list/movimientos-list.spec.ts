import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MovimientosList } from './movimientos-list';

describe('MovimientosList', () => {
  let component: MovimientosList;
  let fixture: ComponentFixture<MovimientosList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovimientosList],
    }).compileComponents();

    fixture = TestBed.createComponent(MovimientosList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
