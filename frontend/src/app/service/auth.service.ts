import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { tap } from "rxjs";
import { environment } from "../../environments/environment";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'jwt';
  private api = inject(HttpClient);

  private readonly apiUrl = environment.apiBaseUrl;

  login(credentials: { username: string; password: string }) {
    return this.api.post<{ token: string }>(`${this.apiUrl}/api/auth/login`, credentials)
      .pipe(tap(res => this.setToken(res.token)));
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  removeToken(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) { return false; }
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();   // kontrola expirace
    } catch {
      return false;
    }
  }
}