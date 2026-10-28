import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LotesList } from './lotes-list';

describe('LotesList', () => {
  let component: LotesList;
  let fixture: ComponentFixture<LotesList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LotesList],
    }).compileComponents();

    fixture = TestBed.createComponent(LotesList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
