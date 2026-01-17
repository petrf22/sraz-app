import { fakeAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { PraniComponent } from './prani.component';

describe('PraniFormComponent', () => {
  let component: PraniComponent;
  let fixture: ComponentFixture<PraniComponent>;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ PraniComponent ]
    })
    .compileComponents();
    ;

    fixture = TestBed.createComponent(PraniComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should compile', () => {
    expect(component).toBeTruthy();
  });
});
