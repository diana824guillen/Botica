import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductoModal } from './producto-modal';

describe('ProductoModal', () => {
  let component: ProductoModal;
  let fixture: ComponentFixture<ProductoModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductoModal],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductoModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
