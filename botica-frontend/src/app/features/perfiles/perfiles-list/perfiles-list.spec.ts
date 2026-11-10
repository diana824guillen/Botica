import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PerfilesList } from './perfiles-list';

describe('PerfilesList', () => {
  let component: PerfilesList;
  let fixture: ComponentFixture<PerfilesList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilesList],
    }).compileComponents();

    fixture = TestBed.createComponent(PerfilesList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
