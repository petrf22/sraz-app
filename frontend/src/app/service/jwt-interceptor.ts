import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { catchError, Observable, throwError } from "rxjs";
import { AuthService } from "../service/auth.service";
import { Router } from "@angular/router";
import { NzMessageService } from 'ng-zorro-antd/message';

@Injectable({ providedIn: 'root' })
export class JwtInterceptor implements HttpInterceptor {
  constructor(private router: Router, private msg: NzMessageService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = inject(AuthService).getToken();

    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }

    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.msg.warning('Přihlášení vypršelo.');
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        }
        return throwError(() => err);
      })
    );
  }
}



