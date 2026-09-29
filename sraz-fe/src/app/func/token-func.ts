import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, Observable, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Požadavky, které token neobnovují (jinak by se zacyklily). */
const NO_REFRESH_PATHS = ['/api/auth/refresh', '/api/auth/otp/', '/api/auth/logout'];

function withAuthHeader(req: HttpRequest<unknown>, accessToken: string | null): HttpRequest<unknown> {
  return accessToken ? req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }) : req;
}

/**
 * Přidá access token; před vypršením ho obnoví a po 401 jednou zkusí obnovit a zopakovat.
 */
export function tokenInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const auth = inject(AuthService);

  if (NO_REFRESH_PATHS.some((path) => req.url.startsWith(path))) {
    return next(withAuthHeader(req, auth.accessToken()));
  }

  return auth.validAccessToken().pipe(
    switchMap((accessToken) =>
      next(withAuthHeader(req, accessToken)).pipe(
        catchError((error: unknown) => {
          if (!accessToken || !(error instanceof HttpErrorResponse) || error.status !== 401 || req.url.startsWith('/api/auth/')) {
            return throwError(() => error);
          }
          return auth.recoverFromUnauthorized(accessToken).pipe(
            switchMap((recovered) => (recovered ? next(withAuthHeader(req, auth.accessToken())) : throwError(() => error))),
          );
        }),
      ),
    ),
  );
}
