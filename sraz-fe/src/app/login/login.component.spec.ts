import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { LoginComponent } from './login.component';
import { AuthService } from '../services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('requests a code and logs in with it', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    component.email.set('hrac@example.com');
    component.consentGiven.set(true);

    component.requestCode();
    expect(component.step()).toBe('code');
    http.expectOne('/api/auth/otp/request').flush({ challengeUid: 'uid-1', expiresInSec: 600, resendAfterSec: 60 });
    expect(component.resendCooldown()).toBe(60);

    component.code.set('123456');
    component.verifyCode();
    const verify = http.expectOne('/api/auth/otp/verify');
    expect(verify.request.body).toEqual({ challengeUid: 'uid-1', code: '123456', email: 'hrac@example.com', termsAccepted: true });
    verify.flush({ accessToken: 'jwt', expiresInSec: 600, newUser: true, email: 'hrac@example.com', roles: ['ROLE_USER'] });

    expect(TestBed.inject(AuthService).isLoggedIn()).toBeTrue();
    expect(navigate).toHaveBeenCalledWith('/', { replaceUrl: true });
  });

  it('goes back to the e-mail step when requesting the code fails', () => {
    component.email.set('hrac@example.com');
    component.requestCode();

    http.expectOne('/api/auth/otp/request').flush(
      { code: 'TOO_MANY_REQUESTS', message: 'Příliš mnoho pokusů' },
      { status: 429, statusText: 'Too Many Requests' },
    );

    expect(component.step()).toBe('email');
    expect(component.errorMessage()).toBe('Příliš mnoho pokusů');
  });
});
