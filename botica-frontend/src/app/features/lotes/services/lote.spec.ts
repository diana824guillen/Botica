import { TestBed } from '@angular/core/testing';

import { Lote } from './lote';

describe('Lote', () => {
  let service: Lote;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Lote);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
