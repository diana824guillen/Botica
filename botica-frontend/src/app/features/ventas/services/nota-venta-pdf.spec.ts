import { TestBed } from '@angular/core/testing';

import { NotaVentaPdf } from './nota-venta-pdf';

describe('NotaVentaPdf', () => {
  let service: NotaVentaPdf;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotaVentaPdf);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
