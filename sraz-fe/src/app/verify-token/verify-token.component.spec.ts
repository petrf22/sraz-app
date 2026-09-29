import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { VerifyTokenComponent } from './verify-token.component';

describe('VerifyTokenComponent', () => {
  let component: VerifyTokenComponent;
  let fixture: ComponentFixture<VerifyTokenComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyTokenComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyTokenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should compile', () => {
    expect(component).toBeTruthy();
  });
});
