import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Token } from '../models/token';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private token = signal<Token | null>(null);

  mailToken(email: string): Observable<void> {
    console.log('UserService :: mailToken :: email:', email);

    const headers = new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded')
    const params = new HttpParams().set('email', email);

    return this.http.post<void>('/api/auth/mail-token', params, { headers });
  }

  verifyToken(emailToken: string) {
    console.log('UserService :: verifyToken :: emailToken:', emailToken);

    return this.http.get<Token>(`/api/auth/verify/${emailToken}`)
      .pipe(
        tap(token => {
          console.log('UserService :: verifyToken :: token:', token);
          this.token.set(token);
        })
      );
  }

  get tokenSig(): Signal<Token | null> {
    return this.token.asReadonly();
  }

  logout(smazatUcet: boolean) {
    console.log('UserService :: logout');
    this.token.set(null);

    const formData = new FormData();

    formData.append('deleteAccount', smazatUcet.toString());

    return this.http.post('/api/auth/logout', formData);
  }

  refreshToken(): Observable<Token> {
    console.log('UserService :: refreshToken');

    return this.http.post<Token>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(
        tap(token => {
          console.log('UserService :: verifyToken :: token:', token);
          this.token.set(token);
        })
      );

  }

}
