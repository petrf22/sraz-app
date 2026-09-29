import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, TokenResponse } from './auth.service';

const token = (accessToken: string, expiresInSec = 600): TokenResponse => ({
  accessToken,
  expiresInSec,
  newUser: false,
  email: 'hrac@example.com',
  roles: ['ROLE_USER'],
});

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-10-01T18:00:00Z'));
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    jasmine.clock().uninstall();
  });

  function login(accessToken = 'jwt-1', expiresInSec = 600): void {
    auth.refresh().subscribe();
    http.expectOne('/api/auth/refresh').flush(token(accessToken, expiresInSec));
  }

  it('anonymous user makes no refresh request', () => {
    let result: string | null | undefined;
    auth.validAccessToken().subscribe((t) => (result = t));
    expect(result).toBeNull();
  });

  it('valid token is returned without calling the server', () => {
    login();
    let result: string | null | undefined;
    auth.validAccessToken().subscribe((t) => (result = t));
    expect(result).toBe('jwt-1');
  });

  it('token is refreshed shortly before it expires', () => {
    login('jwt-1', 600);
    jasmine.clock().tick(580_000);

    let result: string | null | undefined;
    auth.validAccessToken().subscribe((t) => (result = t));
    http.expectOne('/api/auth/refresh').flush(token('jwt-2'));

    expect(result).toBe('jwt-2');
  });

  it('concurrent requests share one refresh (refresh token rotates on the server)', () => {
    login('jwt-1', 600);
    jasmine.clock().tick(600_000);

    const results: (string | null)[] = [];
    auth.validAccessToken().subscribe((t) => results.push(t));
    auth.validAccessToken().subscribe((t) => results.push(t));
    http.expectOne('/api/auth/refresh').flush(token('jwt-2'));

    expect(results).toEqual(['jwt-2', 'jwt-2']);
  });

  it('rejected refresh (401) logs the user out', () => {
    login('jwt-1', 600);
    jasmine.clock().tick(600_000);

    auth.validAccessToken().subscribe();
    http.expectOne('/api/auth/refresh').flush({ code: 'SESSION_EXPIRED' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBeFalse();
  });

  it('network failure during refresh does not log the user out', () => {
    login('jwt-1', 600);
    jasmine.clock().tick(600_000);

    auth.validAccessToken().subscribe();
    http.expectOne('/api/auth/refresh').error(new ProgressEvent('error'));

    expect(auth.isLoggedIn()).toBeTrue();
  });

  it('logout clears the session', () => {
    login();
    auth.logout().subscribe();
    const req = http.expectOne('/api/auth/logout?deleteAccount=false');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(null);
    expect(auth.isLoggedIn()).toBeFalse();
  });
});
