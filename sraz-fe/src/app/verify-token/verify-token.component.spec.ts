import { fakeAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifyTokenComponent } from './verify-token.component';

describe('VerifyTokenComponent', () => {
  let component: VerifyTokenComponent;
  let fixture: ComponentFixture<VerifyTokenComponent>;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ VerifyTokenComponent ]
    })
    .compileComponents();
    ;

    fixture = TestBed.createComponent(VerifyTokenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should compile', () => {
    expect(component).toBeTruthy();
  });
});
