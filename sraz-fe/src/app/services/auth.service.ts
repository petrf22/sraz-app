import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap } from 'rxjs';

/** Odpověď na žádost o přihlašovací kód. */
export interface OtpRequestResponse {
  challengeUid: string;
  expiresInSec: number;
  resendAfterSec: number;
}

/** Access token (JWT) – refresh token je v httpOnly cookie a JavaScript ho nevidí. */
export interface TokenResponse {
  accessToken: string;
  expiresInSec: number;
  newUser: boolean;
  email: string;
  roles: string[];
}

/**
 * Přihlášení kódem z e-mailu (podle aplikace kvalita-cena).
 *
 * Access token je jen v paměti. Při startu aplikace se tiše obnoví přes refresh cookie
 * (proto se uživatel nemusí znovu přihlašovat) a obnovuje se předem – prošlý token backend
 * nebere jako chybu 401, ale jako anonymní požadavek.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly REFRESH_MARGIN_MS = 30_000;
  private static readonly FALLBACK_EXPIRES_IN_SEC = 600;

  private readonly http = inject(HttpClient);
  private readonly session = signal<TokenResponse | null>(null);
  /** Podle Date.now() – správně pozná i prošlý token po uspání počítače. */
  private readonly expiresAt = signal<number | null>(null);
  private refreshInFlight: Observable<string | null> | null = null;

  readonly accessToken = computed(() => this.session()?.accessToken ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly email = computed(() => this.session()?.email ?? null);
  readonly isAdmin = computed(() => this.session()?.roles.includes('ROLE_ADMIN') ?? false);

  /** Platný access token – případně ho nejdřív obnoví. Anonymní uživatel = null bez požadavku na server. */
  validAccessToken(): Observable<string | null> {
    const usable = this.usableAccessToken();
    if (usable !== null) {
      return of(usable);
    }
    if (this.session() === null) {
      return of(null);
    }
    return this.sharedRefresh();
  }

  requestOtp(email: string): Observable<OtpRequestResponse> {
    return this.http.post<OtpRequestResponse>('/api/auth/otp/request', { email });
  }

  verifyOtp(challengeUid: string, code: string, email: string, termsAccepted: boolean): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/otp/verify', { challengeUid, code, email, termsAccepted }, { withCredentials: true })
      .pipe(tap((token) => this.applyToken(token)));
  }

  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(tap((token) => this.applyToken(token)));
  }

  /** Po 401: pokud mezitím jiný požadavek token obnovil, stačí zopakovat; jinak zkusí obnovit. */
  recoverFromUnauthorized(usedToken: string | null): Observable<boolean> {
    const current = this.usableAccessToken();
    if (current !== null && current !== usedToken) {
      return of(true);
    }
    return this.sharedRefresh().pipe(map((token) => token !== null));
  }

  logout(deleteAccount = false): Observable<void> {
    return this.http
      .post<void>(`/api/auth/logout?deleteAccount=${deleteAccount}`, {}, { withCredentials: true })
      .pipe(tap(() => this.clearSession()));
  }

  private usableAccessToken(): string | null {
    const token = this.accessToken();
    const expiresAt = this.expiresAt();
    if (token === null || expiresAt === null) {
      return null;
    }
    return Date.now() < expiresAt - AuthService.REFRESH_MARGIN_MS ? token : null;
  }

  /**
   * Jen jedno obnovení najednou: refresh token se na serveru rotuje a dvě souběžná
   * obnovení by vypadala jako jeho zneužití (zneplatnění přihlášení).
   */
  private sharedRefresh(): Observable<string | null> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const inFlight: Observable<string | null> = this.refresh().pipe(
      map(() => this.accessToken()),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clearSession();
        }
        return of(null); // výpadek sítě uživatele neodhlašuje
      }),
      finalize(() => {
        if (this.refreshInFlight === inFlight) {
          this.refreshInFlight = null;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.refreshInFlight = inFlight;
    return inFlight;
  }

  private applyToken(token: TokenResponse): void {
    this.session.set(token);
    const expiresInSec = Number.isFinite(token.expiresInSec) ? token.expiresInSec : AuthService.FALLBACK_EXPIRES_IN_SEC;
    this.expiresAt.set(Date.now() + expiresInSec * 1000);
  }

  private clearSession(): void {
    this.session.set(null);
    this.expiresAt.set(null);
  }
}

/** Chybová zpráva z odpovědi backendu ({ code, message }). */
export function authErrorMessage(error: unknown): string {
  const body = (error as { error?: { message?: string } })?.error;
  return body?.message ?? 'Něco se nepovedlo, zkuste to prosím znovu.';
}
