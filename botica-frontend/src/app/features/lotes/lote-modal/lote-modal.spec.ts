import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoteModal } from './lote-modal';

describe('LoteModal', () => {
  let component: LoteModal;
  let fixture: ComponentFixture<LoteModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoteModal],
    }).compileComponents();

    fixture = TestBed.createComponent(LoteModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
