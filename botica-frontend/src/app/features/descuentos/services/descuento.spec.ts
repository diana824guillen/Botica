import { TestBed } from '@angular/core/testing';

import { Descuento } from './descuento';

describe('Descuento', () => {
  let service: Descuento;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Descuento);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
