import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoriaModal } from './categoria-modal';

describe('CategoriaModal', () => {
  let component: CategoriaModal;
  let fixture: ComponentFixture<CategoriaModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriaModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriaModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
