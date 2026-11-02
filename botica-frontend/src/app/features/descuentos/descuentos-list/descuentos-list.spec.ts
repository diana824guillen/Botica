import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DescuentosList } from './descuentos-list';

describe('DescuentosList', () => {
  let component: DescuentosList;
  let fixture: ComponentFixture<DescuentosList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DescuentosList],
    }).compileComponents();

    fixture = TestBed.createComponent(DescuentosList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
