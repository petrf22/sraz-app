import { inject } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Při startu aplikace tiše obnoví přihlášení z refresh cookie – uživatel se nemusí
 * při každé návštěvě znovu přihlašovat. Bez platné cookie zůstane anonymní.
 */
export function authInitializer(): () => Observable<unknown> {
  return () => inject(AuthService).refresh().pipe(catchError(() => of(null)));
}
