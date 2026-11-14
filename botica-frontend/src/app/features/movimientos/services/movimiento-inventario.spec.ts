import { TestBed } from '@angular/core/testing';

import { MovimientoInventario } from './movimiento-inventario';

describe('MovimientoInventario', () => {
  let service: MovimientoInventario;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MovimientoInventario);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
