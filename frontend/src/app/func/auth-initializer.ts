
import { inject } from '@angular/core';
import { UserService } from '../services/user-service';
import { catchError, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

export function authInitializer(): () => void | Observable<unknown> | Promise<unknown> {
  return () => {
    const user = inject(UserService);
    const router = inject(Router);

    return user.refreshToken()
      .pipe(
        tap(token => {
          console.log('Auth Initializer :: Token refreshed successfully');
          if (token) {
            router.navigate(['/prani'], { replaceUrl: true });
          } else {
            router.navigate(['/'], { replaceUrl: true });
          }
        }),
        catchError((error) => {
          console.error('Auth Initializer :: Token refresh failed:', error);
          return [];
        }));
  };
}